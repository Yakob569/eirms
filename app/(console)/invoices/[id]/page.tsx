import { InvoiceDetail } from "@/components/console/invoices/InvoiceDetail";

export default async function InvoicePage({
  params,
  searchParams,
}: {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ cancel?: string }>;
}) {
  const { id } = await params;
  const { cancel } = await searchParams;
  // ?cancel=1 comes from "Cancel invoice" in the list's row menu.
  return <InvoiceDetail id={decodeURIComponent(id)} openCancel={cancel === "1"} />;
}
