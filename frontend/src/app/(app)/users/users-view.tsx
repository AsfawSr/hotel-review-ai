"use client";

import { ShieldAlertIcon, UserPlusIcon } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import { PageHeader } from "@/components/page-header";
import { QueryError } from "@/components/query-error";
import { SimpleSelect } from "@/components/simple-select";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { ApiError, dataSourceMode } from "@/lib/data";
import { useCreateUser, useCurrentUser, useIsAdmin, useUpdateUser, useUsers } from "@/lib/data/hooks";
import { formatDate, humanize } from "@/lib/format";
import { USER_ROLES, type AppUser, type UserRole } from "@/lib/types";
import { validateUser } from "@/lib/validation";

const roleOptions = USER_ROLES.map((r) => ({ value: r, label: humanize(r) }));
const ROLE_HELP: Record<UserRole, string> = {
  ADMIN: "Everything, including policies and users",
  MANAGER: "Submit and retry reviews",
  VIEWER: "Read-only access",
};

function CreateUserForm() {
  const create = useCreateUser();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<UserRole>("VIEWER");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const clientErrors = validateUser({ username, password }, true);
    setErrors(clientErrors);
    if (Object.keys(clientErrors).length) return;
    create.mutate(
      { username: username.trim(), password, role },
      {
        onSuccess: (user) => {
          toast.success(`User ${user.username} created.`);
          setUsername("");
          setPassword("");
        },
        onError: (error) => {
          if (error instanceof ApiError && Object.keys(error.fieldErrors).length) setErrors(error.fieldErrors);
          else toast.error(error.message);
        },
      },
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Add user</CardTitle>
        <CardDescription>Passwords are stored as BCrypt hashes on the backend.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} noValidate className="grid gap-4 sm:grid-cols-[1fr_1fr_180px_auto] sm:items-start">
          <div className="space-y-1.5">
            <Label htmlFor="new-username">Username</Label>
            <Input id="new-username" autoComplete="off" value={username} aria-invalid={!!errors.username} onChange={(e) => setUsername(e.target.value)} />
            {errors.username && <p className="text-xs text-destructive">{errors.username}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="new-password">Password</Label>
            <Input id="new-password" type="password" autoComplete="new-password" value={password} aria-invalid={!!errors.password} onChange={(e) => setPassword(e.target.value)} />
            {errors.password && <p className="text-xs text-destructive">{errors.password}</p>}
          </div>
          <SimpleSelect id="new-role" label="Role" value={role} onChange={(v) => setRole(v as UserRole)} options={roleOptions} />
          <Button type="submit" className="sm:mt-6" disabled={create.isPending}>
            <UserPlusIcon />
            Add
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function UserRow({ user, isSelf }: { user: AppUser; isSelf: boolean }) {
  const update = useUpdateUser();
  const change = (input: { role: UserRole; enabled: boolean }) =>
    update.mutate(
      { id: user.id, input },
      { onSuccess: () => toast.success(`Updated ${user.username}.`), onError: (e) => toast.error(e.message) },
    );

  return (
    <TableRow>
      <TableCell className="pl-4 font-medium">
        {user.username} {isSelf && <Badge variant="outline">you</Badge>}
      </TableCell>
      <TableCell>
        <SimpleSelect
          id={`role-${user.id}`}
          ariaLabel={`Role for ${user.username}`}
          className="w-36"
          value={user.role}
          onChange={(v) => change({ role: v as UserRole, enabled: user.enabled })}
          options={roleOptions}
        />
      </TableCell>
      <TableCell className="text-muted-foreground">{ROLE_HELP[user.role]}</TableCell>
      <TableCell>
        <Badge variant={user.enabled ? "secondary" : "outline"}>{user.enabled ? "Active" : "Disabled"}</Badge>
      </TableCell>
      <TableCell className="text-muted-foreground">{formatDate(user.createdAt)}</TableCell>
      <TableCell className="pr-4 text-right">
        <Button variant="outline" size="sm" disabled={update.isPending} onClick={() => change({ role: user.role, enabled: !user.enabled })}>
          {user.enabled ? "Disable" : "Enable"}
        </Button>
      </TableCell>
    </TableRow>
  );
}

export function UsersView() {
  const isAdmin = useIsAdmin();
  const { data: me } = useCurrentUser();
  const { data, isPending, error, refetch } = useUsers(isAdmin);

  if (!isAdmin) {
    return (
      <>
        <PageHeader title="Users" />
        <Alert>
          <ShieldAlertIcon />
          <AlertDescription>Only administrators can manage users.</AlertDescription>
        </Alert>
      </>
    );
  }

  return (
    <>
      <PageHeader title="Users" description="Manage who can access HotelReviewAI and what they can do." />
      <div className="space-y-4">
        {dataSourceMode === "mock" && (
          <Alert>
            <AlertDescription>Demo mode: users are stored in your browser and only the demo account can sign in.</AlertDescription>
          </Alert>
        )}
        <CreateUserForm />
        {error ? (
          <QueryError error={error} onRetry={() => refetch()} />
        ) : (
          <Card className="py-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-4">Username</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Permissions</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead className="pr-4 text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isPending
                  ? Array.from({ length: 3 }, (_, i) => (
                      <TableRow key={i}>
                        <TableCell colSpan={6} className="px-4">
                          <Skeleton className="h-8 w-full" />
                        </TableCell>
                      </TableRow>
                    ))
                  : data.map((user) => <UserRow key={user.id} user={user} isSelf={user.username === me?.username} />)}
              </TableBody>
            </Table>
          </Card>
        )}
      </div>
    </>
  );
}
