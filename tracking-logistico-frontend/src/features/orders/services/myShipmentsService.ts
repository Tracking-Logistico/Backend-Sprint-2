import { api } from "@/lib/api";
import type { MyOrderResponse, OrderStatus, PageResponse } from "@/types/api";

export interface MyShipmentsQuery {
  page: number;
  size: number;
  sort: "fechaCreacion" | "estado";
  direction: "asc" | "desc";
  estado?: OrderStatus;
  fechaDesde?: string;
  fechaHasta?: string;
}

export function getMyShipments(
  query: MyShipmentsQuery,
  token: string,
): Promise<PageResponse<MyOrderResponse>> {
  return api.myOrders(query, token);
}
