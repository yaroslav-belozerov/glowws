# use the official Bun image
# see all versions at https://hub.docker.com/r/oven/bun/tags
FROM oven/bun:1 AS base
WORKDIR /app

# install dependencies into temp directory
# this will cache them and speed up future builds
FROM base AS install
RUN mkdir -p /temp/dev
COPY package.json bun.lock /temp/dev/
RUN cd /temp/dev && bun install

# install with --production (exclude devDependencies)
RUN mkdir -p /temp/prod
COPY package.json bun.lock /temp/prod/
RUN cd /temp/prod && bun install

# copy node_modules from temp directory
# then copy all (non-ignored) project files into the image
FROM base AS prerelease
COPY --from=install /temp/dev/node_modules node_modules
COPY . .
RUN ls

FROM base AS release
ENV NODE_ENV=production
COPY --from=install /temp/prod/node_modules node_modules
COPY --from=prerelease /app/src/index.ts .
COPY --from=prerelease /app/package.json .
COPY --from=prerelease /app/prisma ./prisma/
COPY --from=prerelease /app/prisma.config.ts .

# run the app
RUN mkdir -p /app/src && chown -R bun:bun /app
USER bun
EXPOSE 3030/tcp
ENTRYPOINT ["/bin/sh", "-c", "bunx prisma generate && bunx prisma db push && bun run index.ts"]
