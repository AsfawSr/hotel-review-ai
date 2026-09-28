"use client";

import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

export const ALL = "ALL";

export interface Option {
  value: string;
  label: string;
}

export function SimpleSelect({
  id,
  label,
  value,
  options,
  onChange,
  allLabel,
  className = "w-full",
}: {
  id: string;
  label?: string;
  value: string;
  options: Option[];
  onChange: (value: string) => void;
  allLabel?: string;
  className?: string;
}) {
  const allOptions = allLabel ? [{ value: ALL, label: allLabel }, ...options] : options;
  const items = Object.fromEntries(allOptions.map((o) => [o.value, o.label]));

  return (
    <div className="space-y-1.5">
      {label && <Label htmlFor={id}>{label}</Label>}
      <Select items={items} value={value} onValueChange={(v) => onChange(v ?? ALL)}>
        <SelectTrigger id={id} className={className}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {allOptions.map((o) => (
            <SelectItem key={o.value} value={o.value}>
              {o.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
