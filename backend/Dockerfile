FROM oven/bun:1 AS base
WORKDIR /app

FROM base AS install
RUN mkdir -p /temp/dev
COPY package.json bun.lock /temp/dev/
RUN cd /temp/dev && bun install

RUN mkdir -p /temp/prod
COPY package.json bun.lock /temp/prod/
RUN cd /temp/prod && bun install --production

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

RUN mkdir -p /app/src && chown -R bun:bun /app
USER bun
EXPOSE 3030/tcp
ENTRYPOINT ["/bin/sh", "-c", "bunx prisma generate && bunx prisma db push && bun run index.ts"]
