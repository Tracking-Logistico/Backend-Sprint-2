import { useCallback, useEffect, useState } from "react";
import { getApiError } from "@/lib/api";
import type { MyOrderResponse, OrderStatus, PageResponse } from "@/types/api";
import {
  getMyShipments,
  type MyShipmentsQuery,
} from "../services/myShipmentsService";

const emptyPage: PageResponse<MyOrderResponse> = {
  content: [],
  totalPages: 0,
  totalElements: 0,
  size: 20,
  number: 0,
  first: true,
  last: true,
};

export interface MyShipmentsFilters {
  estado: OrderStatus | "";
  fechaDesde: string;
  fechaHasta: string;
  sort: MyShipmentsQuery["sort"];
  direction: MyShipmentsQuery["direction"];
}

export function useMyShipments(token: string | null) {
  const [filters, setFilters] = useState<MyShipmentsFilters>({
    estado: "",
    fechaDesde: "",
    fechaHasta: "",
    sort: "fechaCreacion",
    direction: "desc",
  });
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(emptyPage);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      setResult(
        await getMyShipments(
          { ...filters, page, size: 20, estado: filters.estado || undefined },
          token,
        ),
      );
    } catch (err) {
      setError(getApiError(err));
    } finally {
      setLoading(false);
    }
  }, [filters, page, token]);

  useEffect(() => {
    const timer = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(timer);
  }, [load]);

  function updateFilters(next: Partial<MyShipmentsFilters>) {
    setPage(0);
    setFilters((current) => ({ ...current, ...next }));
  }

  return {
    ...result,
    filters,
    loading,
    error,
    page,
    reload: load,
    updateFilters,
    setPage,
  };
}
