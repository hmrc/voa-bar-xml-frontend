/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

object BillingAuthorities {

  def find(baCode: String) = billingAuthorities.get(baCode.toUpperCase)

  val billingAuthorities = Map(
    "BA0114" -> "Bath and North East Somerset",
    "BA0116" -> "Bristol",
    "BA0119" -> "South Gloucestershire",
    "BA0121" -> "North Somerset",
    "BA0230" -> "Luton",
    "BA0235" -> "Bedford",
    "BA0240" -> "Central Bedfordshire",
    "BA0335" -> "Bracknell Forest",
    "BA0340" -> "West Berkshire",
    "BA0345" -> "Reading",
    "BA0350" -> "Slough",
    "BA0355" -> "Windsor and Maidenhead",
    "BA0360" -> "Wokingham",
    "BA0405" -> "Aylesbury Vale",
    "BA0410" -> "South Buckinghamshire",
    "BA0415" -> "Chiltern",
    "BA0425" -> "Wycombe",
    "BA0435" -> "Milton Keynes",
    "BA0505" -> "Cambridge",
    "BA0510" -> "East Cambridgeshire",
    "BA0515" -> "Fenland",
    "BA0520" -> "Huntingdonshire",
    "BA0530" -> "South Cambridgeshire",
    "BA0540" -> "Peterborough",
    "BA0650" -> "Halton",
    "BA0655" -> "Warrington",
    "BA0660" -> "Cheshire East",
    "BA0665" -> "Cheshire West and Chester",
    "BA0724" -> "Hartlepool",
    "BA0728" -> "Redcar and Cleveland",
    "BA0734" -> "Middlesbrough",
    "BA0738" -> "Stockton-on-Tees",
    "BA0835" -> "Isles of Scilly",
    "BA0840" -> "Cornwall",
    "BA0905" -> "Allerdale",
    "BA0910" -> "Barrow-in-Furness",
    "BA0915" -> "Carlisle",
    "BA0920" -> "Copeland",
    "BA0925" -> "Eden",
    "BA0930" -> "South Lakeland",
    "BA1005" -> "Amber Valley",
    "BA1010" -> "Bolsover",
    "BA1015" -> "Chesterfield",
    "BA1025" -> "Erewash",
    "BA1030" -> "High Peak",
    "BA1035" -> "North East Derbyshire",
    "BA1040" -> "South Derbyshire",
    "BA1045" -> "Derbyshire Dales",
    "BA1055" -> "Derby",
    "BA1105" -> "East Devon",
    "BA1110" -> "Exeter",
    "BA1115" -> "North Devon",
    "BA1125" -> "South Hams",
    "BA1130" -> "Teignbridge",
    "BA1135" -> "Mid Devon",
    "BA1145" -> "Torridge",
    "BA1150" -> "West Devon",
    "BA1160" -> "Plymouth",
    "BA1165" -> "Torbay",
    "BA1210" -> "Christchurch",
    "BA1215" -> "North Dorset",
    "BA1225" -> "Purbeck",
    "BA1230" -> "West Dorset",
    "BA1235" -> "Weymouth and Portland",
    "BA1240" -> "East Dorset",
    "BA1250" -> "Bournemouth",
    "BA1255" -> "Poole",
    "BA1350" -> "Darlington",
    "BA1355" -> "Durham",
    "BA1410" -> "Eastbourne",
    "BA1415" -> "Hastings",
    "BA1425" -> "Lewes",
    "BA1430" -> "Rother",
    "BA1435" -> "Wealden",
    "BA1445" -> "Brighton and Hove",
    "BA1505" -> "Basildon",
    "BA1510" -> "Braintree",
    "BA1515" -> "Brentwood",
    "BA1520" -> "Castle Point",
    "BA1525" -> "Chelmsford",
    "BA1530" -> "Colchester",
    "BA1535" -> "Epping Forest",
    "BA1540" -> "Harlow",
    "BA1545" -> "Maldon",
    "BA1550" -> "Rochford",
    "BA1560" -> "Tendring",
    "BA1570" -> "Uttlesford",
    "BA1590" -> "Southend-on-Sea",
    "BA1595" -> "Thurrock",
    "BA1605" -> "Cheltenham",
    "BA1610" -> "Cotswold",
    "BA1615" -> "Forest of Dean",
    "BA1620" -> "Gloucester",
    "BA1625" -> "Stroud",
    "BA1630" -> "Tewkesbury",
    "BA1705" -> "Basingstoke and Deane",
    "BA1710" -> "East Hampshire",
    "BA1715" -> "Eastleigh",
    "BA1720" -> "Fareham",
    "BA1725" -> "Gosport",
    "BA1730" -> "Hart",
    "BA1735" -> "Havant",
    "BA1740" -> "New Forest",
    "BA1750" -> "Rushmoor",
    "BA1760" -> "Test Valley",
    "BA1765" -> "Winchester",
    "BA1775" -> "Portsmouth",
    "BA1780" -> "Southampton",
    "BA1805" -> "Bromsgrove",
    "BA1825" -> "Redditch",
    "BA1835" -> "Worcester",
    "BA1840" -> "Wychavon",
    "BA1845" -> "Wyre Forest",
    "BA1850" -> "Herefordshire",
    "BA1860" -> "Malvern Hills",
    "BA1905" -> "Broxbourne",
    "BA1910" -> "Dacorum",
    "BA1915" -> "East Hertfordshire",
    "BA1920" -> "Hertsmere",
    "BA1925" -> "North Hertfordshire",
    "BA1930" -> "St Albans",
    "BA1935" -> "Stevenage",
    "BA1940" -> "Three Rivers",
    "BA1945" -> "Watford",
    "BA1950" -> "Welwyn Hatfield",
    "BA2001" -> "East Riding of Yorkshire",
    "BA2002" -> "North East Lincolnshire",
    "BA2003" -> "North Lincolnshire",
    "BA2004" -> "Kingston upon Hull",
    "BA2100" -> "Isle of Wight",
    "BA2205" -> "Ashford",
    "BA2210" -> "Canterbury",
    "BA2215" -> "Dartford",
    "BA2220" -> "Dover",
    "BA2230" -> "Gravesham",
    "BA2235" -> "Maidstone",
    "BA2245" -> "Sevenoaks",
    "BA2250" -> "Shepway",
    "BA2255" -> "Swale",
    "BA2260" -> "Thanet",
    "BA2265" -> "Tonbridge and Malling",
    "BA2270" -> "Tunbridge Wells",
    "BA2280" -> "Medway",
    "BA2315" -> "Burnley",
    "BA2320" -> "Chorley",
    "BA2325" -> "Fylde",
    "BA2330" -> "Hyndburn",
    "BA2335" -> "Lancaster",
    "BA2340" -> "Pendle",
    "BA2345" -> "Preston",
    "BA2350" -> "Ribble Valley",
    "BA2355" -> "Rossendale",
    "BA2360" -> "South Ribble",
    "BA2365" -> "West Lancashire",
    "BA2370" -> "Wyre",
    "BA2372" -> "Blackburn with Darwen",
    "BA2373" -> "Blackpool",
    "BA2405" -> "Blaby",
    "BA2410" -> "Charnwood",
    "BA2415" -> "Harborough",
    "BA2420" -> "Hinckley and Bosworth",
    "BA2430" -> "Melton",
    "BA2435" -> "North West Leicestershire",
    "BA2440" -> "Oadby and Wigston",
    "BA2465" -> "Leicester",
    "BA2470" -> "Rutland",
    "BA2505" -> "Boston",
    "BA2510" -> "East Lindsey",
    "BA2515" -> "Lincoln",
    "BA2520" -> "North Kesteven",
    "BA2525" -> "South Holland",
    "BA2530" -> "South Kesteven",
    "BA2535" -> "West Lindsey",
    "BA2605" -> "Breckland",
    "BA2610" -> "Broadland",
    "BA2615" -> "Great Yarmouth",
    "BA2620" -> "North Norfolk",
    "BA2625" -> "Norwich",
    "BA2630" -> "South Norfolk",
    "BA2635" -> "Kings Lynn and West Norfolk",
    "BA2705" -> "Craven",
    "BA2710" -> "Hambleton",
    "BA2715" -> "Harrogate",
    "BA2720" -> "Richmondshire",
    "BA2725" -> "Ryedale",
    "BA2730" -> "Scarborough",
    "BA2735" -> "Selby",
    "BA2741" -> "City of York",
    "BA2805" -> "Corby",
    "BA2810" -> "Daventry",
    "BA2815" -> "East Northamptonshire",
    "BA2820" -> "Kettering",
    "BA2825" -> "Northampton",
    "BA2830" -> "South Northamptonshire",
    "BA2835" -> "Wellingborough",
    "BA2935" -> "Northumberland",
    "BA3005" -> "Ashfield",
    "BA3010" -> "Bassetlaw",
    "BA3015" -> "Broxstowe",
    "BA3020" -> "Gedling",
    "BA3025" -> "Mansfield",
    "BA3030" -> "Newark and Sherwood",
    "BA3040" -> "Rushcliffe",
    "BA3060" -> "Nottingham",
    "BA3105" -> "Cherwell",
    "BA3110" -> "Oxford",
    "BA3115" -> "South Oxfordshire",
    "BA3120" -> "Vale of White Horse",
    "BA3125" -> "West Oxfordshire",
    "BA3240" -> "Telford and Wrekin",
    "BA3245" -> "Shropshire",
    "BA3305" -> "Mendip",
    "BA3310" -> "Sedgemoor",
    "BA3315" -> "Taunton Deane",
    "BA3320" -> "West Somerset",
    "BA3325" -> "South Somerset",
    "BA3405" -> "Cannock Chase",
    "BA3410" -> "East Staffordshire",
    "BA3415" -> "Lichfield",
    "BA3420" -> "Newcastle-under-Lyme",
    "BA3425" -> "Stafford",
    "BA3430" -> "South Staffordshire",
    "BA3435" -> "Staffordshire Moorlands",
    "BA3445" -> "Tamworth",
    "BA3455" -> "Stoke",
    "BA3505" -> "Babergh",
    "BA3540" -> "East Suffolk",
    "BA3545" -> "West Suffolk",
    "BA3605" -> "Elmbridge",
    "BA3610" -> "Epsom and Ewell",
    "BA3615" -> "Guildford",
    "BA3620" -> "Mole Valley",
    "BA3625" -> "Reigate and Banstead",
    "BA3630" -> "Runnymede",
    "BA3635" -> "Spelthorne",
    "BA3640" -> "Surrey Heath",
    "BA3645" -> "Tandridge",
    "BA3650" -> "Waverley",
    "BA3655" -> "Woking",
    "BA3705" -> "North Warwickshire",
    "BA3710" -> "Nuneaton and Bedworth",
    "BA3715" -> "Rugby",
    "BA3720" -> "Stratford-upon-Avon",
    "BA3725" -> "Warwick",
    "BA3805" -> "Adur",
    "BA3810" -> "Arun",
    "BA3815" -> "Chichester",
    "BA3820" -> "Crawley",
    "BA3825" -> "Horsham",
    "BA3830" -> "Mid Sussex",
    "BA3835" -> "Worthing",
    "BA3935" -> "Swindon",
    "BA3940" -> "Wiltshire",
    "BA4205" -> "Bolton",
    "BA4210" -> "Bury",
    "BA4215" -> "Manchester",
    "BA4220" -> "Oldham",
    "BA4225" -> "Rochdale",
    "BA4230" -> "Salford",
    "BA4235" -> "Stockport",
    "BA4240" -> "Tameside",
    "BA4245" -> "Trafford",
    "BA4250" -> "Wigan",
    "BA4305" -> "Knowsley",
    "BA4310" -> "Liverpool",
    "BA4315" -> "St Helens",
    "BA4320" -> "Sefton",
    "BA4325" -> "Wirral",
    "BA4405" -> "Barnsley",
    "BA4410" -> "Doncaster",
    "BA4415" -> "Rotherham",
    "BA4420" -> "Sheffield",
    "BA4505" -> "Gateshead",
    "BA4510" -> "Newcastle-upon-Tyne",
    "BA4515" -> "North Tyneside",
    "BA4520" -> "South Tyneside",
    "BA4525" -> "Sunderland",
    "BA4605" -> "Birmingham",
    "BA4610" -> "Coventry",
    "BA4615" -> "Dudley",
    "BA4620" -> "Sandwell",
    "BA4625" -> "Solihull",
    "BA4630" -> "Walsall",
    "BA4635" -> "Wolverhampton",
    "BA4705" -> "Bradford",
    "BA4710" -> "Calderdale",
    "BA4715" -> "Kirklees",
    "BA4720" -> "Leeds",
    "BA4725" -> "Wakefield",
    "BA5030" -> "City of London",
    "BA5060" -> "Barking and Dagenham",
    "BA5090" -> "Barnet",
    "BA5120" -> "Bexley",
    "BA5150" -> "Brent",
    "BA5180" -> "Bromley",
    "BA5210" -> "Camden",
    "BA5240" -> "Croydon",
    "BA5270" -> "Ealing",
    "BA5300" -> "Enfield",
    "BA5330" -> "Greenwich",
    "BA5360" -> "Hackney",
    "BA5390" -> "Hammersmith and Fulham",
    "BA5420" -> "Haringey",
    "BA5450" -> "Harrow",
    "BA5480" -> "Havering",
    "BA5510" -> "Hillingdon",
    "BA5540" -> "Hounslow",
    "BA5570" -> "Islington",
    "BA5600" -> "Kensington and Chelsea",
    "BA5630" -> "Kingston upon Thames",
    "BA5660" -> "Lambeth",
    "BA5690" -> "Lewisham",
    "BA5720" -> "Merton",
    "BA5750" -> "Newham",
    "BA5780" -> "Redbridge",
    "BA5810" -> "Richmond upon Thames",
    "BA5840" -> "Southwark",
    "BA5870" -> "Sutton",
    "BA5900" -> "Tower Hamlets",
    "BA5930" -> "Waltham Forest",
    "BA5960" -> "Wandsworth",
    "BA5990" -> "Westminster",
    "BA6805" -> "Isle of Anglesey",
    "BA6810" -> "Gwynedd",
    "BA6815" -> "Cardiff",
    "BA6820" -> "Ceredigion",
    "BA6825" -> "Carmarthenshire 1",
    "BA6828" -> "Carmarthenshire 2",
    "BA6829" -> "Carmarthenshire 3",
    "BA6830" -> "Denbighshire",
    "BA6835" -> "Flintshire",
    "BA6840" -> "Monmouthshire",
    "BA6845" -> "Pembrokeshire",
    "BA6850" -> "Powys 1 Montgomeryshire",
    "BA6853" -> "Powys 2 Radnorshire",
    "BA6854" -> "Powys 3 Breconshire",
    "BA6855" -> "Swansea",
    "BA6905" -> "Conwy",
    "BA6910" -> "Blaenau Gwent",
    "BA6915" -> "Bridgend",
    "BA6920" -> "Caerphilly",
    "BA6925" -> "Merthyr Tydfil",
    "BA6930" -> "Neath and Port Talbot",
    "BA6935" -> "Newport",
    "BA6940" -> "Rhondda, Cynon, Taff",
    "BA6945" -> "Torfaen",
    "BA6950" -> "Vale of Glamorgan",
    "BA6955" -> "Wrexham",
    "BA3515" -> "Ipswich",
    "BA3520" -> "Mid Suffolk")
}
