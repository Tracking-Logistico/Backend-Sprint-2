import { api } from "@/lib/api";
import type {
  ReschedulingRangeResponse,
  ShipmentTrackingResponse,
} from "@/types/api";

export function getShipmentTracking(
  tracking: string,
  token: string,
): Promise<ShipmentTrackingResponse> {
  return api.getMyShipmentTracking(tracking, token);
}

export function getReschedulingRange(
  tracking: string,
  token: string,
): Promise<ReschedulingRangeResponse> {
  return api.getReschedulingRange(tracking, token);
}

export function rescheduleShipment(
  tracking: string,
  fecha: string,
  token: string,
): Promise<ShipmentTrackingResponse> {
  return api.rescheduleShipment(tracking, fecha, token);
}
