export interface Nutrition {
  calories: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  fiberG: number;
  sugarG: number;
  sodiumMg: number;
}

export interface SnapItem {
  name: string;
  servingQty: number;
  servingUnit: string;
  nutrition: Nutrition;
}

export interface SnapResponse {
  items: SnapItem[];
  confidence: number;
}

export const EMPTY_NUTRITION: Nutrition = {
  calories: 0, proteinG: 0, carbsG: 0, fatG: 0, fiberG: 0, sugarG: 0, sodiumMg: 0,
};
