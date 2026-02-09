/**
 * Test Data Fixtures for WeGo E2E Tests
 *
 * Contains reusable test data for trips, activities, expenses, etc.
 */

export interface TestTrip {
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  coverUrl?: string;
}

export interface TestActivity {
  title: string;
  type: 'ATTRACTION' | 'RESTAURANT' | 'HOTEL' | 'TRANSPORT' | 'OTHER';
  date: string;
  startTime?: string;
  endTime?: string;
  notes?: string;
  place?: TestPlace;
}

export interface TestPlace {
  name: string;
  address: string;
  latitude?: number;
  longitude?: number;
  googlePlaceId?: string;
}

export interface TestExpense {
  title: string;
  amount: number;
  currency: string;
  category: 'FOOD' | 'TRANSPORT' | 'ACCOMMODATION' | 'ACTIVITY' | 'SHOPPING' | 'OTHER';
  splitType: 'EQUAL' | 'EXACT' | 'PERCENTAGE';
}

export interface TestUser {
  id: string;
  email: string;
  name: string;
  picture?: string;
}

// =====================
// Sample Test Data
// =====================

export const sampleTrips: TestTrip[] = [
  {
    title: '東京五日遊',
    description: '探索日本首都的美食與文化',
    startDate: '2026-03-01',
    endDate: '2026-03-05',
  },
  {
    title: '京都賞櫻之旅',
    description: '春天的京都，賞櫻花、遊古寺',
    startDate: '2026-04-01',
    endDate: '2026-04-04',
  },
  {
    title: '台南美食團',
    description: '吃遍台南古早味',
    startDate: '2026-02-15',
    endDate: '2026-02-17',
  },
];

export const sampleActivities: TestActivity[] = [
  {
    title: '淺草寺',
    type: 'ATTRACTION',
    date: '2026-03-01',
    startTime: '09:00',
    endTime: '11:00',
    notes: '東京最古老的寺廟',
    place: {
      name: '淺草寺',
      address: '東京都台東區淺草2-3-1',
      latitude: 35.7148,
      longitude: 139.7967,
    },
  },
  {
    title: '一蘭拉麵',
    type: 'RESTAURANT',
    date: '2026-03-01',
    startTime: '12:00',
    endTime: '13:00',
    notes: '排隊名店，記得早點去',
    place: {
      name: '一蘭拉麵 淺草店',
      address: '東京都台東區淺草1-4-8',
    },
  },
  {
    title: '東京晴空塔',
    type: 'ATTRACTION',
    date: '2026-03-01',
    startTime: '15:00',
    endTime: '18:00',
    place: {
      name: 'Tokyo Skytree',
      address: '東京都墨田區押上1-1-2',
      latitude: 35.7101,
      longitude: 139.8107,
    },
  },
];

export const sampleExpenses: TestExpense[] = [
  {
    title: '午餐 - 一蘭拉麵',
    amount: 3600,
    currency: 'JPY',
    category: 'FOOD',
    splitType: 'EQUAL',
  },
  {
    title: '晴空塔門票',
    amount: 8400,
    currency: 'JPY',
    category: 'ACTIVITY',
    splitType: 'EQUAL',
  },
  {
    title: '計程車 (淺草→晴空塔)',
    amount: 1200,
    currency: 'JPY',
    category: 'TRANSPORT',
    splitType: 'EQUAL',
  },
  {
    title: '紀念品',
    amount: 5000,
    currency: 'JPY',
    category: 'SHOPPING',
    splitType: 'EXACT',
  },
];

export const testUsers: TestUser[] = [
  {
    id: 'test-user-1',
    email: 'alice@test.wego.app',
    name: 'Alice Chen',
    picture: 'https://i.pravatar.cc/150?u=alice',
  },
  {
    id: 'test-user-2',
    email: 'bob@test.wego.app',
    name: 'Bob Wang',
    picture: 'https://i.pravatar.cc/150?u=bob',
  },
  {
    id: 'test-user-3',
    email: 'carol@test.wego.app',
    name: 'Carol Liu',
    picture: 'https://i.pravatar.cc/150?u=carol',
  },
];

// =====================
// Helper Functions
// =====================

/**
 * Generate a random trip with unique title
 */
export function generateRandomTrip(): TestTrip {
  const now = new Date();
  const startDate = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000); // 7 days from now
  const endDate = new Date(startDate.getTime() + 3 * 24 * 60 * 60 * 1000); // 3 days trip

  return {
    title: `測試行程 ${Date.now()}`,
    description: '這是一個自動產生的測試行程',
    startDate: startDate.toISOString().split('T')[0],
    endDate: endDate.toISOString().split('T')[0],
  };
}

/**
 * Generate a random expense
 */
export function generateRandomExpense(amount?: number): TestExpense {
  const categories: TestExpense['category'][] = ['FOOD', 'TRANSPORT', 'ACCOMMODATION', 'ACTIVITY', 'SHOPPING', 'OTHER'];
  const randomCategory = categories[Math.floor(Math.random() * categories.length)];

  return {
    title: `測試支出 ${Date.now()}`,
    amount: amount || Math.floor(Math.random() * 10000) + 100,
    currency: 'TWD',
    category: randomCategory,
    splitType: 'EQUAL',
  };
}

/**
 * Generate a random activity with a date within a trip's date range
 */
export function generateRandomActivity(tripStartDate: string): TestActivity {
  const startDate = new Date(tripStartDate);
  const types: TestActivity['type'][] = ['ATTRACTION', 'RESTAURANT', 'HOTEL', 'TRANSPORT', 'OTHER'];
  const randomType = types[Math.floor(Math.random() * types.length)];

  return {
    title: `測試景點 ${Date.now()}`,
    type: randomType,
    date: startDate.toISOString().split('T')[0],
    startTime: '10:00',
    endTime: '12:00',
    notes: '自動產生的測試景點',
    place: {
      name: `測試地點 ${Date.now()}`,
      address: '台北市中正區忠孝東路一段1號',
      latitude: 25.0418,
      longitude: 121.5199,
    },
  };
}

/**
 * Format date for input fields (YYYY-MM-DD)
 */
export function formatDateForInput(date: Date): string {
  return date.toISOString().split('T')[0];
}

/**
 * Get tomorrow's date
 */
export function getTomorrow(): Date {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow;
}

/**
 * Get date N days from now
 */
export function getDaysFromNow(days: number): Date {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date;
}
