### Back-End Design Notes: General Object Shape
### CRUD
| entity              | creatable | readable | updatable | deletable |
|:--------------------|-----------|----------|-----------|-----------|
| absence             | Y         | Y        | Y         | N         |
| brand               | Y         | Y        | N         | N         |
| client              | Y         | Y        | Y         | N         |
| devolution          | Y         | Y        | N         | N         |
| employee            | Y         | Y        | Y         | N         |
| payment             | Y         | Y        | N         | N         |
| product             | Y         | Y        | Y         | N         |
| product_detail*     | N         | N        | N         | N         |
| purchase            | Y         | Y        | N         | N         |
| sale                | Y         | Y        | N         | N         |
| supplier            | Y         | Y        | N         | N         |
| transaction         | N         | Y        | N         | N         |
| transaction_detail* | N         | N        | N         | N         |
### Table of contents
| Entities                             |
|:-------------------------------------|
| [Base Entity](#entity-base)          |
| [Absence](#entity-absence)           |
| [Brand](#entity-brand)               |
| [Client](#entity-client)             |
| [Employee](#entity-employee)         |
| [Payment](#entity-payment)           |
| [Product](#entity-product)           |
| [Supplier](#entity-supplier)         |
| [Transactions](#entity-transactions) |

### Entity: Base
```puml
Entity {
    id: Int { primary auto }
    createdAt: Instant { auto }
}
```
### Entity: Absence
```puml
// TODO: upper bound for start time
PermissionStatus is Enum {
    PENDING, APPROVED, REJECTED
}

Absence is Entity {
    reason: String
    permissionStatus: PermissionStatus = PENDING
    startTime: Instant i { createdAt <= i <= START_TIME_UPPER_BOUND }
    duration: Int i { 60 <= i<= 480 }
    authorizer: Employee? e { e.role == ADMIN } = null
    employee: Employee
}

Absence create {
    reason
    startTime: Long
    duration
    employeeId: Int
}

Absence read {
    id
    createdAt: Long
    reason
    permissionStatus: String
    startTime: Long
    duration
    authorizer: read of Employee?
    employee: read of Employee
}

Absence update {
    permissionStatus: String s { s == ( "APPROVED" | "REJECTED" ) }
    authorizerId: Int
}
```
### Entity: Brand
```puml
Brand is Entity {
    name: String { unique }
}

Brand create {
    name
}

Brand read {
    name
}

Brand update { disabled }
```
### Entity: Client
```puml
Client is Entity {
    fullName: String
    nid: String { unique }
    address: String
}

Client create {
    fullName
    nid
    address
}

Client read {
    id
    createdAt: Long
    fullName
    nid
    address
}

Client update {
    fullName
    address
}
```
### Entity: Employee
```puml
/* TODO: should password updates go through the normal
         update endpoint? */
Employee is Entity {
    username: String { unique }
    password: String { ... }
    firstName: String
    lastName: String
    nid: String { unique, regex }
    role: Role
}

Employee create {
    username
    password
    firstName
    lastName
    nid
    role: String
}

Employee read {
    id
    createdAt: Long
    username
    firstName
    lastName
    nid
    role: String
}

Employee update {
    username
    firstName
    lastName
    role: String
}
```
### Entity: Payment
```puml
PaymentMethod is Enum {
    CASH, POS 
}

Payment is Entity {
    amount: BigDecimal
    paymentMethod: PaymentMethod
    sale: Sale
}

Payment create {
    amount: String
    method: String
}

Payment read {
    id
    createdAt: Long
    amount: String
    method: String
    sale: read of Sale
}

Payment update { disabled }
```
### Entity: Product
```puml
Product is Entity {
    name: String
    description: String
    buyingPrice: BigDecimal
    sellingPrice: BigDecimal
    brand: Brand
}

Product create {
    name
    description
    buyingPrice: String
    sellingPrice: String
    brandId: Int
}

Product read {
    id
    createdAt: Long
    name
    description
    buyingPrice: String
    sellingPrice: String
    brand: read of Brand
}

Product update {
    buyingPrice: String
    sellingPrice: String
}

ProductDetail is Entity, Detail[Product] {
    product: Product
    sku: String { unique }
    stock: Int
    size: Int
    color: String
}

ProductDetail create {
    sku
    stock
    size
    color
}

ProductDetail read {
    id
    createdAt: Long
    sku
    stock
    size
    color
}

ProductDetail update {
    stock
}
```
### Entity: Supplier
```puml
Supplier is Entity {
    name: String
    nid: String
}

Supplier create {
    name
    nid
}

Supplier read {
    id
    createdAt: Long
    name
    nid
}

Supplier update { disabled }
```
### Entity: Transactions
```puml
Transaction is abstract Entity permitting Sale, Purchase, Devolution {
    employee: Employee
    items: List[TransactionItem]
}

Transaction read {
    id
    createdAt: Long
    employee: read of Employee
    items: List[read of TransactionItem]
}

Transaction create { disabled }
Transaction update { disabled }

TransactionType is Enum { SALE, PURCHASE, DEVOLUTION }

TransactionItem is Entity, Detail[Transaction] {
    transaction: Transaction
    productDetail: ProductDetail
    amount: BigDecimal
    quantity: Int
}

TransactionItem create {
    productDetailId: Int
    quantity
    
    > transactionId: Int
    > transactionType: TransactionType
}

TransactionItem read {
    id
    createdAt: Long
    productDetail: read of ProductDetail
    amount: String
    quantity
}

TransactionItem update { disabled }
```
````puml
Adjustment is Transaction {
    reason: String
}

Adjustment create {
    employeeId: Int
    items: List[create of TransactionItem]
    reason: String
}

Adjustment read {
    id
    createdAt: Long
    employee: read of Employee
    items: List[read of TransactionItem]
    reason
}

Adjustment update { disabled }
````
```puml
Sale is Transaction {
    payments: List[Payment]
    client: Client
}

Sale create {
    employeeId: Int
    items: List[create of TransactionItem]
    payments: List[create of Payment]
    clientId: Int
}

Sale read {
    id
    createdAt: Long
    employee: read of Employee
    items: List[read of TransactionItem]
    payments: List[read of Payment]
    client: read of Client
}

Sale update { disabled }
```
```puml
Purchase is Transaction {
    supplier: Supplier
}

Purchase create {
    employeeId: Int
    items: List[StockDescriptor]
    supplierId: Int
}

Purchase read {
    id
    createdAt: Long
    employee: read of Employee
    items: List[read of TransactionItem]
    supplier: read of Supplier
}

Purchase update { disabled }

StockDescriptor {
    productId: Int
    sku: String
    size: Int
    color: String
    quantity: Int
}
```
```puml
Devolution is Transaction {
    client: Client
    sale: Sale
}

Devolution create {
    employeeId: Int
    items: List[create of TransactionItem]
    clientId: Int
    saleId: Int
}

Devolution read {
    id
    createdAt: Long
    employee: read of Employee
    items: List[read of TransactionItem]
    client: read of Client
    sale: read of Sale
}

Devolution update { disabled }
```