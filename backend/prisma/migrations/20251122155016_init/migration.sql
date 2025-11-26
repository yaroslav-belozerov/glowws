-- CreateEnum
CREATE TYPE "PointType" AS ENUM ('TEXT', 'IMAGE');

-- CreateTable
CREATE TABLE "Idea" (
    "id" SERIAL NOT NULL,
    "priority" INTEGER NOT NULL,
    "isArchived" BOOLEAN NOT NULL,
    "timestampCreated" INTEGER NOT NULL,
    "timestampModified" INTEGER NOT NULL,

    CONSTRAINT "Idea_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Point" (
    "id" SERIAL NOT NULL,
    "parentId" INTEGER NOT NULL,
    "pointContent" TEXT NOT NULL,
    "index" INTEGER NOT NULL,
    "type" "PointType" NOT NULL,
    "isMain" BOOLEAN NOT NULL,

    CONSTRAINT "Point_pkey" PRIMARY KEY ("id")
);

-- AddForeignKey
ALTER TABLE "Point" ADD CONSTRAINT "Point_parentId_fkey" FOREIGN KEY ("parentId") REFERENCES "Idea"("id") ON DELETE CASCADE ON UPDATE CASCADE;
