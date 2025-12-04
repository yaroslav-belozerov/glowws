FROM oven/bun AS build
WORKDIR /app

ENV NODE_ENV=production

COPY package.json bun.lock ./
RUN bun install

COPY prisma ./prisma
RUN bun add @prisma/client
RUN bunx prisma generate
RUN bunx prisma db push 

COPY ./src ./src

RUN bun build \
	--compile \
	--minify-whitespace \
	--minify-syntax \
	--target=bun \
	--outfile server \
	./src/index.ts

FROM debian:12-slim
RUN apt-get update -y && apt-get install -y openssl && rm -rf /var/lib/apt/lists/*
WORKDIR /app

COPY --from=build /app/server server

ENV NODE_ENV=production
CMD ["./server"]
EXPOSE 3030
