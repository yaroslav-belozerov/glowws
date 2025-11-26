import {Elysia, t, ValidationError} from "elysia";
import {jwt} from '@elysiajs/jwt'
import 'dotenv/config'
import {PrismaPg} from '@prisma/adapter-pg'
import {PrismaClient} from './generated/prisma/client'
import bcrypt from 'bcrypt';

const connectionString = `${process.env.DATABASE_URL}`;

const adapter = new PrismaPg({connectionString})
const db = new PrismaClient({
    adapter
})

const app = new Elysia()
    .use(
        jwt({
            name: 'jwt',
            secret: 'Fischl von Luftschloss Narfidort'
        })
    )
    .onError(({error, status}) => {
        if (error instanceof ValidationError) {
            console.log(error)
            return status(400, 'Bad Request')
        }
    })
    .post('/sign-in', async ({jwt, status, cookie: {auth}, body: {username, password}}) => {
        const user = await db.user.findUnique({where: {username}})
        if (user != null) {
            const compare = await bcrypt.compare(password, user.passwordHash)
            if (compare === true) {
                const value = await jwt.sign({username})
                auth.set({
                    value,
                    httpOnly: true,
                    maxAge: 7 * 86400,
                })
                return status(200, value)
            } else {
                return status(401, 'Unauthorized')
            }
        } else {
            return status(404, 'Not Found')
        }
    }, {
        body: t.Object({
            username: t.String(),
            password: t.String()
        }, {error: 'Login body must contain username and password'})
    })
    .post('/sign-up', async ({jwt, status, cookie: {auth}, body: {username, password}}) => {
        const user = await db.user.findUnique({where: {username}})
        if (user == null) {
            const passwordHash = await bcrypt.hash(password, 10);
            await db.user.create({
                data: {
                    username, passwordHash
                }
            })
            const value = await jwt.sign({username, passwordHash})
            auth.set({
                value,
                httpOnly: true,
                maxAge: 7 * 86400,
                path: '/',
            })
            return status(200, value)
        } else {
            return status(409, 'Conflict')
        }
    }, {
        body: t.Object({
            username: t.String(),
            password: t.String()
        }, {error: 'Login body must contain username and password'})
    })
    .get("/ideas", async ({jwt, status, cookie: {auth}, query: {search}}) => {
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        if (search == undefined) {
            return db.idea.findMany({
                where: {
                    ownerUsername: `${profile.username}`
                },
                orderBy: {
                    timestampModified: 'desc'
                }
            });
        }
        return db.idea.findMany({
            where: {
                ownerUsername: `${profile.username}`, points: {
                    some: {
                        pointContent: {
                            contains: `${search}`
                        }
                    }
                }
            },
            orderBy: {
                timestampModified: 'desc'
            }
        });
    })
    .get("/points/:ideaId", async ({jwt, status, cookie: {auth}, params: {ideaId}}) => {
        const parentId = Number.parseInt(ideaId)
        if (Number.isNaN(parentId)) {
            return status(400, 'Bad Request')
        }
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        return db.point.findMany({where: {parentId}});
    })
    .post("/points/:ideaId", async ({jwt, status, cookie: {auth}, params: {ideaId}}) => {
        const parentId = Number.parseInt(ideaId)
        if (Number.isNaN(parentId)) {
            return status(400, 'Bad Request')
        }
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        await db.$executeRaw`UPDATE "Idea"
                             SET "timestampModified" = DEFAULT
                             WHERE id = ${parentId}`;
        return db.point.create({
            data: {
                parentId: parentId,
            }
        });
    })
    .delete("/points/:pointId", async ({jwt, status, cookie: {auth}, params: {pointId}}) => {
        const id = Number.parseInt(pointId)
        if (Number.isNaN(id)) {
            return status(400, 'Bad Request')
        }
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        const deleted = await db.point.delete({
            where: {
                id
            }
        });
        await db.$executeRaw`UPDATE "Idea"
                             SET "timestampModified" = DEFAULT
                             WHERE id = ${deleted.parentId}`;
        return db.point.findMany({where: {parentId: deleted.parentId}});
    })
    .put("/points/:pointId", async ({jwt, status, cookie: {auth}, params: {pointId}, body: {pointContent, isMain}}) => {
        const id = Number.parseInt(pointId)
        if (Number.isNaN(id)) {
            return status(400, 'Bad Request')
        }
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        const updated = await db.point.update({
            where: {
                id
            },
            data: {
                pointContent, isMain
            }
        });
        await db.$executeRaw`UPDATE "Idea"
                             SET "timestampModified" = DEFAULT
                             WHERE id = ${updated.parentId}`;
        return db.point.findMany({where: {parentId: updated.parentId}});
    }, {body: t.Object({pointContent: t.String(), isMain: t.Boolean()})})
    .put("/ideas/:id/priority/:priority", async ({jwt, status, cookie: {auth}, params: {id, priority}}) => {
        const intId = Number.parseInt(id)
        const intP = Number.parseInt(priority)
        if (Number.isNaN(intId) || Number.isNaN(intP)) {
            return status(400, 'Bad Request')
        }
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        await db.$executeRaw`UPDATE "Idea"
                             SET "priority"          = ${intP},
                                 "timestampModified" = DEFAULT
                             WHERE id = ${intId}`;
        return db.idea.findMany({where: {ownerUsername: `${profile.username}`}});
    })
    .put("/ideas/archive/:id", async ({jwt, status, cookie: {auth}, params: {id}}) => {
        const intId = Number.parseInt(id)
        if (Number.isNaN(intId)) {
            return status(400, 'Bad Request')
        }
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        await db.$executeRaw`UPDATE "Idea"
                             SET "isArchived"        = NOT "isArchived",
                                 "timestampModified" = DEFAULT
                             WHERE id = ${intId}`;
        return db.idea.findMany({where: {ownerUsername: `${profile.username}`}});
    })
    .put("/ideas/archive/all", async ({jwt, status, cookie: {auth}, body}) => {
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        await db.$transaction(
            body.map((id) =>
                db.$executeRaw`UPDATE "Idea"
                             SET "isArchived"        = NOT "isArchived",
                                 "timestampModified" = DEFAULT
                             WHERE id = ${id}`
            )
        )
        return db.idea.findMany({where: {ownerUsername: `${profile.username}`}});
    }, {body: t.Array(t.Number())})
    .post("/ideas", async ({jwt, status, cookie: {auth}}) => {
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        return db.idea.create({
            data: {
                ownerUsername: `${profile.username}`,
            }
        });
    })
    .listen(3000);

console.log(
    `🦊 Elysia is running at ${app.server?.hostname}:${app.server?.port}`
);