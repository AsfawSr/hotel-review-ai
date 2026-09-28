import type { Review, Sentiment, Topic } from "@/lib/types";
import { composeManagerResponse } from "./analysis";

type SeedEntry = {
  guestName: string;
  rating: number | null;
  daysAgo: number;
  hour: number;
  reviewText: string;
  sentiment: Sentiment;
  sentimentScore: number;
  topics: Topic[];
  failed?: string;
};

const SEED: SeedEntry[] = [
  { guestName: "Emma Johansson", rating: 5, daysAgo: 0, hour: 9, reviewText: "Absolutely loved our stay. The room was spotless and the staff at reception were incredibly friendly. Highly recommend!", sentiment: "POSITIVE", sentimentScore: 94, topics: ["CLEANLINESS", "STAFF"] },
  { guestName: "Liam O'Connor", rating: 2, daysAgo: 1, hour: 22, reviewText: "The room next to ours had a party until 2am and nobody from security came despite two calls. Very poor night's sleep.", sentiment: "NEGATIVE", sentimentScore: 18, topics: ["NOISE", "SAFETY"] },
  { guestName: "Sofia Rossi", rating: 4, daysAgo: 1, hour: 11, reviewText: "Great breakfast buffet with lots of fresh fruit and a proper espresso machine. Room was a bit small but comfortable.", sentiment: "POSITIVE", sentimentScore: 78, topics: ["FOOD", "COMFORT"] },
  { guestName: "Noah Williams", rating: 3, daysAgo: 2, hour: 15, reviewText: "Check-in took almost 40 minutes because only one person was at the desk. Once in the room everything was fine.", sentiment: "NEUTRAL", sentimentScore: 45, topics: ["CHECK_IN", "STAFF"] },
  { guestName: "Aisha Rahman", rating: 5, daysAgo: 3, hour: 18, reviewText: "Perfect location, five minutes walk from the old town and the train station. We walked everywhere.", sentiment: "POSITIVE", sentimentScore: 90, topics: ["LOCATION"] },
  { guestName: "Lucas Müller", rating: 1, daysAgo: 3, hour: 8, reviewText: "Dirty bathroom, hair in the shower and stained towels. Worst hotel experience I've had. Never again.", sentiment: "NEGATIVE", sentimentScore: 4, topics: ["CLEANLINESS"] },
  { guestName: "Chloé Martin", rating: 4, daysAgo: 4, hour: 20, reviewText: "The spa and pool were lovely and not crowded. Wi-Fi was slow in the evenings though.", sentiment: "POSITIVE", sentimentScore: 70, topics: ["AMENITIES"] },
  { guestName: "Mateo García", rating: 3, daysAgo: 5, hour: 13, reviewText: "Decent hotel for the price. Nothing special but nothing bad either.", sentiment: "NEUTRAL", sentimentScore: 55, topics: ["VALUE"] },
  { guestName: "Hannah Schmidt", rating: 5, daysAgo: 6, hour: 10, reviewText: "Staff went above and beyond for my mother's birthday — cake and flowers in the room. Thank you so much!", sentiment: "POSITIVE", sentimentScore: 97, topics: ["STAFF"] },
  { guestName: "Yuki Tanaka", rating: 4, daysAgo: 7, hour: 16, reviewText: "Very clean and quiet room. The bed was extremely comfortable. Breakfast could have more Asian options.", sentiment: "POSITIVE", sentimentScore: 76, topics: ["CLEANLINESS", "COMFORT", "FOOD"] },
  { guestName: "Oliver Brown", rating: 2, daysAgo: 8, hour: 12, reviewText: "Elevator was out of order for two days and we were on the 6th floor with a stroller. Not accessible at all.", sentiment: "NEGATIVE", sentimentScore: 22, topics: ["ACCESSIBILITY", "AMENITIES"] },
  { guestName: "Isabella Silva", rating: 5, daysAgo: 9, hour: 19, reviewText: "Amazing rooftop restaurant with a view of the whole city. The dinner was excellent.", sentiment: "POSITIVE", sentimentScore: 93, topics: ["FOOD", "LOCATION"] },
  { guestName: "Ethan Clarke", rating: 3, daysAgo: 10, hour: 14, reviewText: "Late check-out was refused even though the hotel looked empty. Otherwise an okay stay.", sentiment: "NEUTRAL", sentimentScore: 42, topics: ["CHECK_OUT"] },
  { guestName: "Mia Novak", rating: 4, daysAgo: 11, hour: 9, reviewText: "Friendly staff and a good gym. Parking is expensive.", sentiment: "POSITIVE", sentimentScore: 68, topics: ["STAFF", "AMENITIES", "VALUE"] },
  { guestName: "Ahmed Hassan", rating: 1, daysAgo: 12, hour: 23, reviewText: "Receptionist was rude when I asked about my reservation and charged me twice. Still waiting for a refund.", sentiment: "NEGATIVE", sentimentScore: 8, topics: ["STAFF", "VALUE"] },
  { guestName: "Grace Lee", rating: 5, daysAgo: 13, hour: 17, reviewText: "Our third time here and it never disappoints. Everything is always clean and the team remembers us.", sentiment: "POSITIVE", sentimentScore: 95, topics: ["CLEANLINESS", "STAFF"] },
  { guestName: "Daniel Kowalski", rating: 3, daysAgo: 15, hour: 11, reviewText: "Room was fine, but the air conditioning was noisy and hard to control.", sentiment: "NEUTRAL", sentimentScore: 40, topics: ["COMFORT", "NOISE"] },
  { guestName: "Zara Ahmed", rating: 4, daysAgo: 16, hour: 8, reviewText: "Nice modern design and a great location near the museum district.", sentiment: "POSITIVE", sentimentScore: 80, topics: ["LOCATION"] },
  { guestName: "Jack Thompson", rating: 2, daysAgo: 18, hour: 21, reviewText: "Breakfast ran out of most items by 9am and nobody refilled them. Coffee was cold.", sentiment: "NEGATIVE", sentimentScore: 25, topics: ["FOOD"] },
  { guestName: "Lea Dubois", rating: 5, daysAgo: 19, hour: 10, reviewText: "Wheelchair accessible room was perfect — roll-in shower and plenty of space. Staff helped with everything.", sentiment: "POSITIVE", sentimentScore: 92, topics: ["ACCESSIBILITY", "STAFF"] },
  { guestName: "Samuel Okafor", rating: 4, daysAgo: 21, hour: 15, reviewText: "Smooth check-in via the app, key was ready when we arrived. Room a little dated.", sentiment: "POSITIVE", sentimentScore: 66, topics: ["CHECK_IN", "COMFORT"] },
  { guestName: "Nora Lindqvist", rating: 3, daysAgo: 23, hour: 12, reviewText: "Pool area was nice but closed for maintenance on one of our three days.", sentiment: "NEUTRAL", sentimentScore: 50, topics: ["AMENITIES"] },
  { guestName: "Carlos Mendes", rating: 5, daysAgo: 25, hour: 18, reviewText: "Excellent value for money. Big room, great view, and the minibar was included.", sentiment: "POSITIVE", sentimentScore: 91, topics: ["VALUE", "COMFORT"] },
  { guestName: "Ella Fischer", rating: 2, daysAgo: 27, hour: 7, reviewText: "The door lock was broken and it took hours to get it fixed. I did not feel safe leaving my things.", sentiment: "NEGATIVE", sentimentScore: 20, topics: ["SAFETY"] },
  { guestName: "Ravi Patel", rating: 4, daysAgo: 29, hour: 13, reviewText: "Good vegetarian options at the restaurant and very helpful concierge.", sentiment: "POSITIVE", sentimentScore: 74, topics: ["FOOD", "STAFF"] },
  { guestName: "Amelia Wright", rating: null, daysAgo: 31, hour: 16, reviewText: "Stayed one night for a conference. Meeting rooms were well equipped.", sentiment: "NEUTRAL", sentimentScore: 58, topics: ["AMENITIES"] },
  { guestName: "Finn Andersen", rating: 5, daysAgo: 34, hour: 11, reviewText: "Quiet, clean, comfortable. Exactly what I needed after a long flight.", sentiment: "POSITIVE", sentimentScore: 88, topics: ["NOISE", "CLEANLINESS", "COMFORT"] },
  { guestName: "Olivia Rossi", rating: 3, daysAgo: 37, hour: 20, reviewText: "Checkout was quick, but there was an unexpected city tax on the bill that nobody mentioned.", sentiment: "NEUTRAL", sentimentScore: 44, topics: ["CHECK_OUT", "VALUE"] },
  { guestName: "Kenji Watanabe", rating: 4, daysAgo: 40, hour: 9, reviewText: "Housekeeping was thorough and the room smelled fresh every day.", sentiment: "POSITIVE", sentimentScore: 79, topics: ["CLEANLINESS"] },
  { guestName: "Maya Cohen", rating: 1, daysAgo: 43, hour: 22, reviewText: "Construction noise from 7am every day and no warning at booking. Terrible.", sentiment: "NEGATIVE", sentimentScore: 6, topics: ["NOISE"] },
  { guestName: "Leo Bianchi", rating: 5, daysAgo: 46, hour: 14, reviewText: "The best hotel breakfast I've ever had. Fresh pastries and made-to-order omelettes.", sentiment: "POSITIVE", sentimentScore: 96, topics: ["FOOD"] },
  { guestName: "Sara Nilsson", rating: 4, daysAgo: 50, hour: 17, reviewText: "Lovely stay, beds were comfy. Would appreciate more power outlets near the bed.", sentiment: "POSITIVE", sentimentScore: 72, topics: ["COMFORT"] },
  { guestName: "Tomás Herrera", rating: 3, daysAgo: 54, hour: 10, reviewText: "Staff were polite but seemed overworked. Room service took over an hour.", sentiment: "NEUTRAL", sentimentScore: 41, topics: ["STAFF", "FOOD"] },
  { guestName: "Julia Nowak", rating: 5, daysAgo: 58, hour: 19, reviewText: "Fantastic spa treatments and a very relaxing atmosphere overall.", sentiment: "POSITIVE", sentimentScore: 89, topics: ["AMENITIES"] },
  { guestName: "Benjamin Adler", rating: 2, daysAgo: 63, hour: 8, reviewText: "Overpriced for what you get. Tiny room, thin walls.", sentiment: "NEGATIVE", sentimentScore: 24, topics: ["VALUE", "NOISE"] },
  { guestName: "Chen Wei", rating: 4, daysAgo: 68, hour: 12, reviewText: "Easy to reach from the airport and close to the business district.", sentiment: "POSITIVE", sentimentScore: 75, topics: ["LOCATION"] },
  { guestName: "Lucy Evans", rating: 5, daysAgo: 74, hour: 15, reviewText: "Kids loved the pool and the staff were so patient with them. Great family hotel.", sentiment: "POSITIVE", sentimentScore: 93, topics: ["AMENITIES", "STAFF"] },
  { guestName: "Pierre Laurent", rating: 3, daysAgo: 80, hour: 18, reviewText: "Average experience. Clean room but the decor feels tired.", sentiment: "NEUTRAL", sentimentScore: 52, topics: ["CLEANLINESS", "COMFORT"] },
  { guestName: "Fatima Zahra", rating: 4, daysAgo: 85, hour: 11, reviewText: "Welcoming reception team and quick check-in even late at night.", sentiment: "POSITIVE", sentimentScore: 81, topics: ["CHECK_IN", "STAFF"] },
  { guestName: "Viktor Petrov", rating: 2, daysAgo: 1, hour: 6, reviewText: "Room smelled of smoke even though it was a non-smoking floor.", sentiment: "NEGATIVE", sentimentScore: 20, topics: ["CLEANLINESS"], failed: "Ollama request timed out after 60000 ms." },
  { guestName: "Ingrid Olsen", rating: 4, daysAgo: 2, hour: 20, reviewText: "Good stay overall, the concierge booked us a great restaurant nearby.", sentiment: "POSITIVE", sentimentScore: 77, topics: ["STAFF", "FOOD"], failed: "Failed to parse model output as JSON: Unexpected token at position 0." },
];

function at(now: Date, daysAgo: number, hour: number): string {
  const d = new Date(now);
  d.setDate(d.getDate() - daysAgo);
  d.setHours(hour, (daysAgo * 17) % 60, 0, 0);
  if (d > now) d.setTime(now.getTime() - (hour + 1) * 5 * 60_000);
  return d.toISOString();
}

export function buildSeedReviews(now: Date = new Date()): Review[] {
  const reviews = SEED.map((entry): Omit<Review, "id"> => {
    const submittedAt = at(now, entry.daysAgo, entry.hour);
    const analyzedAt = new Date(new Date(submittedAt).getTime() + 8_000).toISOString();
    const base = {
      guestName: entry.guestName,
      reviewText: entry.reviewText,
      rating: entry.rating,
      submittedAt,
      updatedAt: analyzedAt,
      analysisUpdatedAt: analyzedAt,
    };
    if (entry.failed) {
      return { ...base, analysisStatus: "FAILED", analysisError: entry.failed, analysis: null };
    }
    return {
      ...base,
      analysisStatus: "COMPLETED",
      analysisError: null,
      analysis: {
        sentiment: entry.sentiment,
        sentimentScore: entry.sentimentScore,
        topics: entry.topics,
        mainTopic: entry.topics[0],
        managerResponse: composeManagerResponse(entry.guestName, entry.sentiment, entry.topics[0]),
        source: "AI",
        modelName: "llama3.2:latest",
        promptVersion: "review-analysis-v2",
        createdAt: analyzedAt,
        updatedAt: analyzedAt,
      },
    };
  });

  return reviews
    .sort((a, b) => a.submittedAt.localeCompare(b.submittedAt))
    .map((review, index) => ({ ...review, id: index + 1 }));
}
