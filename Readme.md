# AUTO BARS v2


## Pre-Requisite

- [Service Manager][service-manager] - you can look at this [page][install-sm] also
- [sbt][]

[service-manager]: https://confluence.tools.tax.service.gov.uk/display/DTRG/Service+Manager+Setup
[install-sm]: https://github.com/hmrc/service-manager/wiki/Install#install-service-manager
[sbt]: https://www.scala-sbt.org/download.html

## Run

### Dependencies

>sm --start VOA_BAR

Ensure that the following dependencies has been started with service manager:
- AUTH
- CONTACT_FRONTEND (only needed for the feedback page)
- DATASTREAM
- UPSCAN_STUB
- VOA-AUTOBARS-STUBS
- VOA_AUTOBARS_LEGACY_EBARS
- VOA_BAR_BACKEND



You also must have a local Mongo Server running.

### Autobars V2

Once you've started all your dependencies, you can simply run, in your terminal

>sbt run

Then open the following link into your browser:
http://localhost:8448/voa-bar-xml-frontend

## Web Form

To complete the webform, login with the  Brighton code - BA1445 see https://github.com/hmrc/voa-bar-xml-frontend/blob/master/app/models/BillingAuthorities.scala#L98
