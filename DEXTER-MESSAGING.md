# Dexter Send — One-Way Notifications

Dexter Send is a one-way notification system for Dexter customers and Dexter team devices.

## Purpose
Only two message types are required:
1. Order notifications
2. Promotional messages

Customers do not reply inside Dexter Send.

## Sender
Every Dexter-owned notification must visibly identify the sender as **Dexters**.

Examples:
- Dexters — Your order has been accepted
- Dexters — Your order is being prepared
- Dexters — Your order is ready for collection
- Dexters — Sunday Roast orders are now open
- Dexters — This week's offer is now live

## Free delivery channels
Dexter Send should prefer internet-based delivery so there is no SMS charge per message:
- Dexter Loyalty app/PWA push notifications
- Dexter customer app push notifications
- Dexter OS team-device notifications
- in-app notification inbox
- web push where supported

These channels use Wi-Fi/mobile data. They avoid SMS carrier fees, although backend hosting and push infrastructure can still have normal service costs.

## No-reply design
- No customer reply box
- No inbound SMS handling
- No two-way chat requirement
- No reply phone number required
- Notifications deep-link to the relevant Dexter page where useful
- Promotional notifications include the required marketing preference / opt-out controls

## Order notification events
- order received
- order accepted
- order amended
- order rejected
- cooking / preparing
- ready for collection
- collected
- payment / deposit reminder where appropriate

## Promotions
- offers
- meal deals
- Sunday Roast
- seasonal menus
- loyalty rewards
- opening-hours announcements
- events and fundraising

## Delivery priority
1. Push notification
2. In-app inbox
3. Email fallback where enabled

SMS is not part of the free default route.

## Branding
App notification title: **Dexters**
Notification icon: Dexter logo
Notification deep-link: the relevant Dexter app/order/offer screen

## Consent
Order notifications are transactional service messages.
Promotional messages are sent only where the customer has the required marketing preference recorded.
