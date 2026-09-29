import type { ReviewAnalysis, Sentiment, Topic } from "@/lib/types";

// TypeScript port of ReviewAnalysisProcessingService#buildFallbackAnalysis.

const containsAny = (text: string, ...keywords: string[]) => keywords.some((k) => text.includes(k));
const countMatches = (text: string, ...keywords: string[]) => keywords.filter((k) => text.includes(k)).length;
const clamp = (value: number) => Math.min(100, Math.max(0, value));

function estimateTextOnlyScore(text: string): number {
  let score = 50;
  score += 10 * countMatches(text, "great", "excellent", "amazing", "friendly", "clean", "love", "comfortable", "happy");
  score -= 10 * countMatches(text, "bad", "dirty", "rude", "noisy", "uncomfortable", "slow", "terrible", "hate", "poor");
  if (containsAny(text, "don't like", "dont like", "didn't like", "not good", "never again", "worst")) score -= 25;
  if (containsAny(text, "really liked", "loved", "highly recommend", "very satisfied")) score += 20;
  return clamp(score);
}

export function estimateSentimentScore(text: string, rating: number | null): number {
  const textScore = estimateTextOnlyScore(text);
  if (rating == null) return textScore;

  const ratingScore = clamp(rating * 20);
  const strongNegative = containsAny(text, "don't like", "dont like", "didn't like", "not good", "never again", "terrible", "awful", "worst");
  const strongPositive = containsAny(text, "really liked", "loved", "excellent", "amazing", "very happy", "highly recommend", "great stay");

  let blended = Math.round(ratingScore * 0.55 + textScore * 0.45);
  if (strongNegative && rating >= 4) {
    blended = Math.min(40, Math.round(ratingScore * 0.35 + textScore * 0.65));
  } else if (strongPositive && rating <= 2) {
    blended = Math.max(60, Math.round(ratingScore * 0.35 + textScore * 0.65));
  }
  return clamp(blended);
}

export function detectTopics(text: string): Topic[] {
  const topics: Topic[] = [];
  if (containsAny(text, "clean", "dirty", "hygiene")) topics.push("CLEANLINESS");
  if (containsAny(text, "staff", "service", "friendly", "rude")) topics.push("STAFF");
  if (containsAny(text, "location", "near", "distance", "area")) topics.push("LOCATION");
  if (containsAny(text, "amenities", "pool", "spa", "gym", "wifi")) topics.push("AMENITIES");
  if (containsAny(text, "value", "price", "cost", "expensive", "cheap")) topics.push("VALUE");
  if (containsAny(text, "food", "breakfast", "restaurant", "dining")) topics.push("FOOD");
  if (containsAny(text, "noise", "noisy", "quiet")) topics.push("NOISE");
  if (containsAny(text, "bed", "comfortable", "pillow", "room temperature")) topics.push("COMFORT");
  if (containsAny(text, "check-in", "check in", "reception", "arrival")) topics.push("CHECK_IN");
  if (containsAny(text, "check-out", "checkout", "check out", "departure")) topics.push("CHECK_OUT");
  if (containsAny(text, "wheelchair", "elevator", "lift", "accessible", "ramp")) topics.push("ACCESSIBILITY");
  if (containsAny(text, "safe", "security", "lock", "unsafe")) topics.push("SAFETY");
  return topics.length ? topics : ["OTHER"];
}

export const sentimentFromScore = (score: number): Sentiment =>
  score >= 65 ? "POSITIVE" : score <= 35 ? "NEGATIVE" : "NEUTRAL";

const TOPIC_PHRASES: Record<Topic, string> = {
  CLEANLINESS: "the cleanliness of your room",
  STAFF: "your interactions with our team",
  ACCESSIBILITY: "accessibility during your stay",
  FOOD: "our dining experience",
  LOCATION: "our location",
  AMENITIES: "our amenities",
  VALUE: "the value of your stay",
  COMFORT: "the comfort of your room",
  CHECK_IN: "your check-in experience",
  CHECK_OUT: "your check-out experience",
  NOISE: "noise levels during your stay",
  SAFETY: "safety and security",
  OTHER: "your stay",
};

export function composeManagerResponse(guestName: string, sentiment: Sentiment, mainTopic: Topic): string {
  const firstName = guestName.trim().split(/\s+/)[0] || "Guest";
  const phrase = TOPIC_PHRASES[mainTopic];
  switch (sentiment) {
    case "POSITIVE":
      return `Dear ${firstName}, thank you so much for your kind words about ${phrase}. It is wonderful to hear you enjoyed your time with us, and I will make sure the team sees your feedback. We look forward to welcoming you back soon.`;
    case "NEGATIVE":
      return `Dear ${firstName}, I sincerely apologise that ${phrase} did not meet your expectations. This is not the standard we aim for, and I have shared your comments with the responsible team so we can act on them right away. I would welcome the chance to speak with you directly and make this right.`;
    default:
      return `Dear ${firstName}, thank you for sharing balanced feedback about ${phrase}. We are glad parts of your stay went well and have noted where we can do better. We hope to offer you an even better experience next time.`;
  }
}

export function analyzeReview(guestName: string, reviewText: string, rating: number | null, now: string): ReviewAnalysis {
  const text = reviewText.toLowerCase();
  const sentimentScore = estimateSentimentScore(text, rating);
  const sentiment = sentimentFromScore(sentimentScore);
  const topics = detectTopics(text);
  return {
    sentiment,
    sentimentScore,
    topics,
    mainTopic: topics[0],
    managerResponse: composeManagerResponse(guestName, sentiment, topics[0]),
    // The demo runs the backend's keyword heuristic, so label it honestly.
    source: "FALLBACK",
    modelName: "heuristic",
    promptVersion: null,
    language: null,
    createdAt: now,
    updatedAt: now,
  };
}
