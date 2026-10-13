import { api } from "@/lib/api";
import type {
  IncidentType,
  IncidentsResponse,
  RegisterIncidentPayload,
  RegisterIncidentResponse,
} from "@/types/api";

export function getIncidentTypes(token: string): Promise<IncidentType[]> {
  return api.incidentTypes(token);
}

export function getOrderIncidents(
  orderId: number,
  token: string,
): Promise<IncidentsResponse> {
  return api.orderIncidents(orderId, token);
}

export function createIncident(
  orderId: number,
  payload: RegisterIncidentPayload,
  token: string,
): Promise<RegisterIncidentResponse> {
  return api.registerIncident(orderId, payload, token);
}
