import re

with open('app/src/main/java/com/rushit/app/data/order/OrderRepository.kt', 'r') as f:
    content = f.read()

# Add paymentMethod to OrderDraft
content = content.replace("val discountPaise: Long = 0", "val discountPaise: Long = 0,\n    val paymentMethod: String = \"COD\"")

# Add paymentMethod and paymentStatus to Order constructor in placeOrder
order_ctor = """                addressSummary = draft.addressSummary,
                paymentMethod = draft.paymentMethod,
                paymentStatus = if (draft.paymentMethod == "Cash on Delivery" || draft.paymentMethod == "COD") "Pending" else "Success",
                isDemo = true,"""
content = re.sub(
    r'                addressSummary = draft.addressSummary,\n                isDemo = true,',
    order_ctor,
    content
)

# Update encode
encode_block = """        put("demo", order.isDemo)
        put("paymentMethod", order.paymentMethod)
        put("paymentStatus", order.paymentStatus)"""
content = re.sub(
    r'        put\("demo", order.isDemo\)',
    encode_block,
    content
)

# Update decode
decode_block = """            addressSummary = json.optString("address").takeIf { it.isNotBlank() && it != "null" },
            paymentMethod = json.optString("paymentMethod", "COD"),
            paymentStatus = json.optString("paymentStatus", "Pending"),
            isDemo = json.optBoolean("demo", true),"""
content = re.sub(
    r'            addressSummary = json.optString\("address"\).takeIf \{ it.isNotBlank\(\) && it != "null" \},\n            isDemo = json.optBoolean\("demo", true\),',
    decode_block,
    content
)

# Also update OrderLine encode/decode for variantId and variantName
# encode
orderline_encode = """                    put("productId", line.productId)
                    put("variantId", line.variantId)
                    put("variantName", line.variantName)"""
content = re.sub(
    r'                    put\("productId", line.productId\)',
    orderline_encode,
    content
)

# decode
orderline_decode = """                        OrderLine(
                            productId = line.optString("productId"),
                            variantId = line.optString("variantId").takeIf { it.isNotBlank() && it != "null" },
                            variantName = line.optString("variantName").takeIf { it.isNotBlank() && it != "null" },"""
content = re.sub(
    r'                        OrderLine\(\n                            productId = line.optString\("productId"\),',
    orderline_decode,
    content
)

with open('app/src/main/java/com/rushit/app/data/order/OrderRepository.kt', 'w') as f:
    f.write(content)
