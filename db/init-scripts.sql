create extension hstore;
create schema budgets;
create table if not exists budgets."User" ("id" BIGSERIAL NOT NULL PRIMARY KEY,"name" VARCHAR NOT NULL);
create table if not exists budgets."Budgets"("id" BIGSERIAL NOT NULL PRIMARY KEY, "title" VARCHAR NOT NULL, "description" VARCHAR NOT NULL,
"amount" BIGINT NOT NULL, "US_ID" BIGINT NOT NULL);
create table if not exists budgets."Expenses"("id" BIGSERIAL NOT NULL PRIMARY KEY,"amount" BIGINT NOT NULL, "description"VARCHAR NOT NULL, "LIN_ID"BIGINT NOT NULL);
create table if not exists budgets."LineItems"("id" BIGSERIAL NOT NULL PRIMARY KEY,"BUDGETCATEGORY" VARCHAR NOT NULL, "projectedAmount" BIGINT NOT NULL,"totalAmountSpent" BIGINT NOT NULL,"BUD_ID" BIGINT NOT NULL);
