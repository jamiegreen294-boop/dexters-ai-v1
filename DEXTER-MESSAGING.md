# Dexter Messaging Platform

Dexter Messaging is the communications layer for Dexter OS and the wider Dexter customer experience.

## 1. Dexter Messages — team messaging
A private internet-based messaging app for the Dexter team, similar in use to iMessage/WhatsApp but owned by Dexter.

### Core features
- 1-to-1 staff chat
- team/group chats
- manager announcements
- read receipts
- typing indicators
- message reactions
- photo/file sharing
- voice notes
- message search
- staff directory
- role-based groups
- device notifications
- optional disappearing messages for non-record business chat
- audit/retention rules for business-critical channels
- owner/admin moderation tools

### Cost model
Messages travel over Wi-Fi/mobile data through Dexter's backend. There is no per-message SMS charge for Dexter-to-Dexter messages. Hosting/storage still has infrastructure cost, so "free" means no carrier/SMS fee per message, not zero operating cost forever.

## 2. Dexter Chat — customer messaging
A customer-facing chat system for the Loyalty app / website / future Dexter customer app.

### Customer experience
- message Dexter from the Loyalty app
- message from the website
- order/help questions
- send photos
- automated Dexter AI first response
- staff takeover
- conversation history
- order/customer context
- push notifications when the customer has the app/PWA installed

### Staff experience
- unified inbox
- customer profile beside conversation
- unread / assigned / waiting / resolved states
- manager escalation
- canned replies
- AI reply suggestions
- staff takeover from AI

## 3. Dexter Send — outbound communications
Dexter Send is the Dexter-owned campaign and notification service.

### Free/no-carrier-cost channels
- in-app messages
- web push notifications
- PWA push notifications
- Dexter Messages team broadcasts
- customer app notifications
- email from Dexter-owned domains (subject to email provider/infrastructure limits)

### Paid carrier channels
- SMS
- MMS
- RCS where provider billing applies
- WhatsApp Business messaging outside free service windows / according to provider pricing

Dexter Send must always show the true channel cost before a paid-carrier campaign is sent.

## Sender identity
For Dexter-owned app, push and email channels, the visible sender can be "Dexter" or "Dexters" because Dexter controls the app/service.

For SMS, an alphanumeric sender ID such as "DEXTER" may be possible through an SMS provider in supported destinations, but it is not the same as owning a phone number and usually cannot receive replies. Availability and registration rules depend on the network/provider/country.

## Architecture
- Supabase/Postgres conversation store
- realtime message delivery
- push notification service
- Dexter AI routing
- owner/admin permissions
- customer/staff identity mapping
- attachment storage
- delivery/read receipts
- campaign engine
- templates
- opt-out/consent handling
- retention/audit policy

## Safety / permissions
- customer marketing must respect recorded consent and opt-out
- transactional and service messages must be distinguishable from marketing
- staff access is role controlled
- owner can suspend accounts/devices
- destructive/admin actions require owner approval
