import type { Topic } from "@/lib/types";

export interface MockPolicy {
  title: string;
  category: string;
  content: string;
  topics: Topic[];
}

export const MOCK_POLICIES: MockPolicy[] = [
  {
    title: "Housekeeping Standards",
    category: "Housekeeping",
    content:
      "Rooms are cleaned daily between 9:00 and 15:00. Any cleanliness complaint must be addressed within 30 minutes, and guests may be offered a room change or a complimentary amenity.",
    topics: ["CLEANLINESS", "COMFORT"],
  },
  {
    title: "Guest Service Recovery",
    category: "Front Office",
    content:
      "Staff are empowered to resolve issues up to a value of 100 EUR without manager approval. Negative experiences with staff should receive a personal follow-up from the duty manager within 24 hours.",
    topics: ["STAFF", "OTHER"],
  },
  {
    title: "Check-in and Check-out",
    category: "Front Office",
    content:
      "Standard check-in is from 15:00 and check-out until 11:00. Early check-in and late check-out are subject to availability and are free for loyalty members.",
    topics: ["CHECK_IN", "CHECK_OUT"],
  },
  {
    title: "Food & Beverage Quality",
    category: "Restaurant",
    content:
      "Breakfast is served from 6:30 to 10:30. Dietary requirements (vegan, gluten-free, halal) must be accommodated on request. Complaints about food quality are escalated to the head chef.",
    topics: ["FOOD"],
  },
  {
    title: "Quiet Hours",
    category: "Operations",
    content:
      "Quiet hours are from 22:00 to 07:00. Security responds to noise complaints within 15 minutes. Guests affected by noise may be moved to a quieter room at no cost.",
    topics: ["NOISE", "SAFETY"],
  },
  {
    title: "Facilities & Amenities",
    category: "Facilities",
    content:
      "The pool and gym are open 7:00-22:00. Wi-Fi is complimentary in all areas. Maintenance issues with amenities must be logged and resolved within one business day.",
    topics: ["AMENITIES"],
  },
  {
    title: "Accessibility Commitment",
    category: "Facilities",
    content:
      "Accessible rooms include roll-in showers and grab bars. Elevators are inspected monthly. Guests with accessibility needs are offered assistance on arrival.",
    topics: ["ACCESSIBILITY"],
  },
  {
    title: "Pricing & Value Guarantee",
    category: "Revenue",
    content:
      "We match lower published rates for identical stays. Billing disputes are reviewed by the finance team within 48 hours.",
    topics: ["VALUE", "LOCATION"],
  },
];

export function buildPolicyContext(topics: Topic[]): string {
  const matches = MOCK_POLICIES.filter((p) => p.topics.some((t) => topics.includes(t))).slice(0, 3);
  const docs = matches.length ? matches : [MOCK_POLICIES[1]];
  return docs.map((p) => `Title: ${p.title}\nCategory: ${p.category}\nContent: ${p.content}`).join("\n\n");
}
