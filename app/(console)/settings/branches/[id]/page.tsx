import { BranchDetail } from "@/components/console/settings/Branches";

export default async function BranchPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <BranchDetail id={decodeURIComponent(id)} />;
}
