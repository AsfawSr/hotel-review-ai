"use client";

import { DownloadIcon, UploadIcon } from "lucide-react";
import Link from "next/link";
import { useRef, useState } from "react";
import { toast } from "sonner";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button, buttonVariants } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { useImportReviews } from "@/lib/data/hooks";
import { IMPORT_MAX_BYTES, IMPORT_MAX_ROWS } from "@/lib/review-import";
import type { ImportResult } from "@/lib/types";

const TEMPLATE = 'guestName,reviewText,rating\nJane Doe,"Lovely stay, spotless room",5\nJohn Smith,Breakfast was cold,2\n';
const TEMPLATE_HREF = `data:text/csv;charset=utf-8,${encodeURIComponent(TEMPLATE)}`;

export function ImportReviewsCard() {
  const importReviews = useImportReviews();
  const inputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ImportResult | null>(null);

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!file) return;
    if (file.size > IMPORT_MAX_BYTES) {
      toast.error("The file is larger than 2 MB.");
      return;
    }
    setResult(null);
    importReviews.mutate(file, {
      onSuccess: (data) => {
        setResult(data);
        setFile(null);
        if (inputRef.current) inputRef.current.value = "";
        toast.success(`Imported ${data.imported} review${data.imported === 1 ? "" : "s"}.`);
      },
      onError: (error) => toast.error(error.message),
    });
  };

  return (
    <Card className="max-w-2xl">
      <CardHeader>
        <CardTitle>Bulk import (CSV)</CardTitle>
        <CardDescription>
          Columns <code>guestName</code>, <code>reviewText</code> and optional <code>rating</code> (aliases like guest, review,
          stars work too). Up to {IMPORT_MAX_ROWS} rows, 2 MB.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-5">
        <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="flex-1 space-y-1.5">
            <Label htmlFor="importFile">CSV file</Label>
            <Input
              id="importFile"
              ref={inputRef}
              type="file"
              accept=".csv,text/csv"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </div>
          <div className="flex gap-2">
            <a href={TEMPLATE_HREF} download="reviews-template.csv" className={buttonVariants({ variant: "outline" })}>
              <DownloadIcon />
              Template
            </a>
            <Button type="submit" disabled={!file || importReviews.isPending}>
              <UploadIcon />
              {importReviews.isPending ? "Importing…" : "Import"}
            </Button>
          </div>
        </form>

        {result && (
          <Alert variant={result.skipped ? "destructive" : "default"} aria-live="polite">
            <AlertTitle>
              {result.imported} imported, {result.skipped} skipped
            </AlertTitle>
            <AlertDescription>
              {result.imported > 0 && (
                <span>
                  Imported reviews are queued for analysis. <Link href="/reviews" className="underline">View reviews</Link>
                </span>
              )}
            </AlertDescription>
          </Alert>
        )}

        {result && result.errors.length > 0 && (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-20">Line</TableHead>
                <TableHead>Problem</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {result.errors.map((error) => (
                <TableRow key={`${error.line}-${error.message}`}>
                  <TableCell className="tabular-nums">{error.line}</TableCell>
                  <TableCell className="whitespace-normal">{error.message}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
