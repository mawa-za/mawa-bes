# Procurement, Sales and Inventory V2

This implementation moves quotations, purchase orders and stock execution away from the older generic transaction-only screens into dedicated V2 flows.

## Main process flows

### Quote to Sales Order

1. `POST /v2/quotations`
2. `POST /v2/quotations/{id}/status` with `SENT` or `ACCEPTED`
3. `POST /v2/quotations/{id}/convert-to-sales-order`
4. `POST /v2/sales-orders/{id}/reserve`
5. `POST /v2/sales-orders/{id}/issue`

### Purchase Order to Goods Receipt and Putaway

1. `POST /v2/purchase-orders`
2. `POST /v2/purchase-orders/{id}/status` with `SENT`
3. `POST /v2/purchase-orders/{id}/goods-receipt`
4. `POST /v2/putaways`

### Direct Goods Receipt

Use `POST /v2/goods-receipts` for non-PO stock receipts.

## New document tables

- `quotation`
- `quotation_line`
- `purchase_order`
- `purchase_order_line`

The existing stock tables remain in use:

- `goods_receipt`
- `goods_receipt_line`
- `putaway`
- `putaway_line`
- `stock_balance`
- `stock_movement`
- `sales_order`
- `sales_order_line`

## Number ranges

The migration adds these number range objects where missing:

- `QUOTATION` → `QT`
- `PURCHASE_ORDER` → `PO`
- `GOODS_RECEIPT` → `GRN`
- `PUTAWAY` → `PUT`
- `STOCK_MOVEMENT` → `STM`
- `STOCK_ISSUE` → `ISS`
- `SALES_ORDER` → `SO`

## Notes

- Sales order creation checks available stock but does not reserve it automatically. Use the `reserve` action to commit available stock.
- Sales order issue decrements on-hand stock and consumes reservations first.
- Purchase order receipt updates `purchase_order_line.received_qty`, `open_qty` and PO status.
- Goods receipt stock lands into the receiving location first. Putaway moves it to a final bin/location.
