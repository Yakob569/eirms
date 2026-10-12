import { SourceSystemDetail } from "@/components/console/settings/SourceSystems";

export default async function SourceSystemPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <SourceSystemDetail id={decodeURIComponent(id)} />;
}
