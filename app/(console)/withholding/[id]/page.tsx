import { WithholdingDetail } from "@/components/console/withholding/WithholdingDetail";

export default async function WithholdingRecordPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <WithholdingDetail id={decodeURIComponent(id)} />;
}
