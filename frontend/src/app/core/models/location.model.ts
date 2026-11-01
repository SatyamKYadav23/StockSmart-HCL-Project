export interface Location {
  id: number;
  name: string;
  address: string | null;
  locationType: string;
  createdAt: string;
}

export interface LocationRequest {
  name: string;
  address?: string | null;
  locationType: string;
}
