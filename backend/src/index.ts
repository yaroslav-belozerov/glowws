import {Elysia, t, ValidationError} from "elysia";
import {jwt} from '@elysiajs/jwt'
import 'dotenv/config'
import {PrismaPg} from '@prisma/adapter-pg'
import {PointType, Prisma, PrismaClient} from './generated/prisma/client'
import bcrypt from 'bcryptjs';
import IdeaWhereInput = Prisma.IdeaWhereInput;
import IdeaOrderByWithRelationInput = Prisma.IdeaOrderByWithRelationInput;
import SortOrder = Prisma.SortOrder;
import staticPlugin from "@elysiajs/static";

const connectionString = `${process.env.GWWS_DATABASE_URL}`;

const adapter = new PrismaPg({connectionString})
const db = new PrismaClient({
    adapter
})

const app = new Elysia()
    .use(
        jwt({
            name: 'jwt',
            secret: `${process.env.GWWS_JWT_SECRET}`
        })
    )
    .use(
        staticPlugin()
    )
    .onError(({error, status}) => {
        console.log(error)
        if (error instanceof ValidationError) {
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
    .get("/ideas", async ({jwt, status, cookie: {auth}, query: {search, sort, sort_dir, filter}}) => {
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        return getIdeasMainScreen(profile.username, search, sort, sort_dir, filter)
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
        return db.point.findMany({where: {parentId}, orderBy: {index: 'asc'}});
    })
    .delete("/ideas/:ideaId", async ({
                                         jwt,
                                         status,
                                         cookie: {auth},
                                         params: {ideaId},
                                         query: {search, sort, sort_dir, filter}
                                     }) => {
        const id = Number.parseInt(ideaId)
        if (Number.isNaN(id)) {
            return status(400, 'Bad Request')
        }
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        await db.idea.delete({where: {id}})
        return getIdeasMainScreen(profile.username, search, sort, sort_dir, filter)
    })
    .delete("/ideas/all", async ({jwt, status, cookie: {auth}, body, query: {search, sort, sort_dir, filter}}) => {
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        await db.$transaction(
            body.map((id) => db.idea.delete({where: {id}}))
        )
        return getIdeasMainScreen(profile.username, search, sort, sort_dir, filter)
    }, {body: t.Array(t.Number())})
    .post("/points", async ({jwt, status, cookie: {auth}, body: {parentId, index, content: pointContent, type}}) => {
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }
        await db.$executeRaw`UPDATE "Idea"
                             SET "timestampModified" = DEFAULT
                             WHERE id = ${parentId}`;
        await db.$executeRaw`UPDATE "Point"
                             SET "index" = "index" + 1
                             WHERE "parentId" = ${parentId}
                               AND "index" >= ${index}`;
        return db.point.create({
            data: {
                parentId,
                index,
                pointContent,
                type
            }
        });
    }, {body: t.Object({parentId: t.Number(), index: t.Number(), content: t.String(), type: t.Enum(PointType)})})
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
        const file = Bun.file(`./${deleted.pointContent}`)
        if (await file.exists()) {
            await file.delete()
        }

        await db.$executeRaw`UPDATE "Idea"
                             SET "timestampModified" = DEFAULT
                             WHERE id = ${deleted.parentId}`;
        return db.point.findMany({where: {parentId: deleted.parentId}, orderBy: {index: 'asc'}});
    })
    .put("/points/:pointId/main", async ({jwt, status, cookie: {auth}, params: {pointId}, body: {isMain}}) => {
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
            where: {id},
            data: {isMain}
        })
        await db.$executeRaw`UPDATE "Idea"
                             SET "timestampModified" = DEFAULT
                             WHERE "id" = ${updated.parentId}`;
        return db.point.findMany({where: {parentId: updated.parentId}, orderBy: {index: 'asc'}});
    }, {body: t.Object({isMain: t.Boolean()})})
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
        return db.point.findMany({where: {parentId: updated.parentId}, orderBy: {index: 'asc'}});
    }, {body: t.Object({pointContent: t.String(), isMain: t.Boolean()})})
    .put("/ideas/:id/priority/:priority", async ({
                                                     jwt,
                                                     status,
                                                     cookie: {auth},
                                                     params: {id, priority},
                                                     query: {search, sort, sort_dir, filter}
                                                 }) => {
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
        return getIdeasMainScreen(profile.username, search, sort, sort_dir, filter)
    })
    .put("/ideas/archive/:id", async ({
                                          jwt,
                                          status,
                                          cookie: {auth},
                                          params: {id},
                                          query: {search, sort, sort_dir, filter}
                                      }) => {
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
        return getIdeasMainScreen(profile.username, search, sort, sort_dir, filter)
    })
    .put("/ideas/archive/all", async ({jwt, status, cookie: {auth}, body, query: {search, sort, sort_dir, filter}}) => {
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
        return getIdeasMainScreen(profile.username, search, sort, sort_dir, filter)
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
    .post("/upload", async ({jwt, status, cookie: {auth}, body}) => {
        // @ts-ignore
        const profile = await jwt.verify(auth.value)
        if (!profile) {
            return status(401, 'Unauthorized')
        }

        const split = body.filename.split(".")
        const old = split[0].replaceAll("/", "").replaceAll(".", "").toLowerCase()
        const ext = split[split.length - 1];
        const newFileName = `${old}${Date.now()}.${ext}`
        const path = `public/${profile.username}/${newFileName}`
        await Bun.write(`./${path}`, await body.file.arrayBuffer())
        return path
    }, {body: t.Object({file: t.File(), filename: t.String()})})
    .listen(3030);

console.log(
    `🦊 Elysia is running at ${app.server?.hostname}:${app.server?.port}`
);

const getIdeasMainScreen = (username: any, search: any, sort: any, sort_dir: any, filter: any) => {
    const order = matchSort(sort, sort_dir);
    const where = matchFilter(filter, username, search)
    return db.idea.findMany({
        where: where,
        orderBy: order,
        include: {
            points: {
                orderBy: [{isMain: 'desc'}, {index: 'asc'}],
                select: {
                    pointContent: true,
                    type: true
                },
                take: 1
            }
        }
    });
}

function matchFilter(filter: any, owner: any, search: any): IdeaWhereInput {
    const query = `${search}`.replaceAll("%", () => "\\%").replaceAll("_", () => "\\_")
    const points = (search == undefined || search == "" ? {} : {
        some: {
            pointContent: {
                contains: `%${query}%`
            }
        }
    })

    const pairs = `${filter}`.split(",")
    const priorityList = pairs.map((it) => {
        const [k, v] = it.replaceAll("[", "").replaceAll("]", "").trim().split("=")
        const num = Number.parseInt(v)
        if (k == "priority" && !Number.isNaN(num)) {
            return num
        }
    }).filter((it) => it != undefined)
    const priority = priorityList.length == 0 ? {} : {in: priorityList}

    return {
        priority,
        points,
        ownerUsername: `${owner}`
    }
}

function matchSortDirection(sortDir: any): SortOrder {
    switch (sortDir) {
        case "ascending":
            return "asc"
        case "descending":
            return "desc"
        default:
            return "desc"
    }
}

function matchSort(sortType: any, sortDir: string): IdeaOrderByWithRelationInput {
    const sortDirType: SortOrder = matchSortDirection(sortDir)
    switch (sortType) {
        case "timestamp_modified":
            return {timestampModified: sortDirType}
        case "timestamp_created":
            return {timestampCreated: sortDirType}
        case "priority":
            return {priority: sortDirType}
        default:
            return {timestampModified: sortDirType}
    }
}
