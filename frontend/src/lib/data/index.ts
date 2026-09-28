import { apiDataSource } from "./api-source";
import type { DataSource, DataSourceMode } from "./data-source";
import { mockDataSource } from "./mock-source";

export const dataSourceMode: DataSourceMode = process.env.NEXT_PUBLIC_DATA_SOURCE === "api" ? "api" : "mock";

export const dataSource: DataSource = dataSourceMode === "api" ? apiDataSource : mockDataSource;

export type { DataSource, DataSourceMode } from "./data-source";
export { ApiError, isNotFound, isUnauthorized } from "./errors";
