import { InvoiceReceipt } from "@/components/console/invoices/InvoiceReceipt";

export default async function InvoiceReceiptPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <InvoiceReceipt id={decodeURIComponent(id)} />;
}
