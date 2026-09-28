import { InvoiceDetail } from "@/components/console/invoices/InvoiceDetail";

export default async function InvoicePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <InvoiceDetail id={decodeURIComponent(id)} />;
}
