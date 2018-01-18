#!/bin/bash

echo "Applying migration CouncilTaxStart"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /councilTaxStart                       controllers.CouncilTaxStartController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "councilTaxStart.title = councilTaxStart" >> ../conf/messages.en
echo "councilTaxStart.heading = councilTaxStart" >> ../conf/messages.en

echo "Moving test files from generated-test/ to test/"
rsync -avm --include='*.scala' -f 'hide,! */' ../generated-test/ ../test/
rm -rf ../generated-test/

echo "Migration CouncilTaxStart completed"
