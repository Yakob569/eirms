import { ApiKeyDetail } from "@/components/console/settings/ApiKeys";

export default async function ApiKeyPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ApiKeyDetail id={decodeURIComponent(id)} />;
}
