import { ReceiptDetail } from "@/components/console/receipts/ReceiptDetail";

export default async function ReceiptPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <ReceiptDetail id={decodeURIComponent(id)} />;
}
