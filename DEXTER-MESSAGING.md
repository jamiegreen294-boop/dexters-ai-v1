# Dexter Send — Hybrid Free Messaging + SMS Fallback

Dexter Send should feel like SMS to staff, but use a free internet route whenever possible.

## Important technical rule
A normal SMS sent to an arbitrary mobile number is carried by the mobile network and cannot be made universally free by Dexter. iMessage is free between Apple users because both devices use Apple's internet messaging service rather than SMS.

Dexter Send therefore uses the same model:

### Free Dexter route
When a customer has the Dexter Loyalty app/PWA or future Dexter customer app:
- send over Wi-Fi/mobile data
- show a normal push notification from **Dexters**
- keep a notification/message history in the Dexter app
- no SMS carrier fee per message
- one-way only for order updates and promotions

### SMS fallback
When the customer does not have a reachable Dexter app/push subscription:
- Dexter Send may fall back to SMS
- sender should request the brand name **DEXTERS** where supported
- no reply is required
- SMS provider/carrier charges still apply
- show the expected cost before bulk promotional sends

## Staff experience
Staff should not have to choose the transport manually.

They choose the customer/message and Dexter Send decides:
1. Dexter push/data message — free route
2. in-app inbox
3. email fallback if enabled
4. SMS fallback only when needed

The message composer should display:
- Free delivery
- SMS fallback required
- Estimated paid SMS count/cost

## Message types
### Order notifications
- order received
- accepted
- amended
- rejected
- preparing
- ready for collection
- collected

### Promotions
- offers
- meal deals
- Sunday Roast
- seasonal menus
- loyalty rewards
- opening-hours announcements
- events/fundraising

## Branding
- App/push sender: **Dexters**
- Notification icon: Dexter logo
- SMS sender requested: **DEXTERS** where supported by provider/network
- No-reply design

## Consent
Order notifications are transactional service messages.
Promotional messages require the customer's recorded marketing preference and opt-out handling.
