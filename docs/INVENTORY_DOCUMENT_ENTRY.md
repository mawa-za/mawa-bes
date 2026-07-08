# Inventory document entry

This release upgrades the stock module to capture inventory commercial documents in an invoice-style flow.

Implemented documents:
- Quotations
- Purchase Orders
- Goods Receipts
- Sales Orders

Key behaviour:
- Each document is saved with multiple line items in one request/transaction.
- Product/customer/supplier/warehouse/location values are selected from lookup-driven UI controls.
- Quotation, Purchase Order, Goods Receipt and Sales Order headers store subtotal, VAT and total amounts.
- Goods Receipts now also support VAT fields on header and line level.
- Purchase Order receiving can prefill open PO lines and receive multiple lines at once.
- Sales Orders support reserve and issue actions against stock balances.
