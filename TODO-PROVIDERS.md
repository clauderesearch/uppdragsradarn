# TODO: Consultant Provider Implementations

Based on the list from https://annaleijon.se/lista-pa-konsultmaklare-i-stockholm.html

## Currently Implemented ✓
- Emagine Consulting (EmagineProvider.java)
- Ework/Verama (EworkProvider.java)
- Experis (ExperisProvider.java)
- A Society Group (ASocietyProvider.java)

## TODO - High Priority (Active Job Boards)

### 1. Aliant
- **URL**: https://aliant.recman.se/
- **Portal**: Recman-based system
- **Notes**: Appears to be standard job board

### 2. Amendo
- **URL**: https://jobb.amendo.se/jobs
- **Notes**: Modern job portal

### 3. Biolit
- **URL**: https://biolit.se/konsultuppdrag/
- **Notes**: Tech consulting focus

### 4. Capacify
- **URL**: https://www.capacify.se/interimsuppdrag
- **Notes**: Interim assignments

### 5. Centric
- **URL**: https://careers.centric.eu/se/underkonsult/
- **Notes**: Subcontractor positions

### 6. Developers Bay
- **URL**: https://developersbay.se/frilansare/
- **Notes**: Standard job board

### 7. Digitalenta
- **URL**: https://www.digitalenta.se/lediga-jobb-uppdrag-digital-marknadsforing-e-handel
- **Notes**: Digital marketing focus

### 8. DJV Consulting
- **URL**: https://djvconsulting.se/partner/#uppdrag
- **Notes**: Partner network

### 9. Enkl
- **URL**: https://enkl.se/
- **Notes**: General consulting

### 10. Enmanskonsulterna
- **URL**: https://enmanskonsulterna.se/
- **Notes**: Solo consultants

### 11. Epico
- **URL**: https://jobs.epico.se/jobs
- **Notes**: IT consulting

### 12. Evolution Jobs
- **URL**: https://evolutionjobs.com/eu/job-search/
- **Notes**: International focus

### 13. Fasticon
- **URL**: https://fasticon.se/lediga-jobb/
- **Notes**: IT consulting

### 14. Finance Recruitment
- **URL**: https://www.financerecruitment.se/lediga-jobb/
- **Notes**: Finance sector

### 15. Gameboost
- **URL**: https://www.gameboost.se/jobs
- **Notes**: Gaming industry

### 16. Gigs for Her
- **URL**: https://gigsforher.se/aktuella-gigs/
- **Notes**: Women in tech focus

### 17. Great IT
- **URL**: https://jobb.greatit.se/#jobs
- **Notes**: IT consulting

### 18. Hiroy
- **URL**: https://hiroy.se/
- **Notes**: Tech recruitment

### 19. House of Skills
- **URL**: https://www.houseofskills.se/konsultuppdrag/
- **Notes**: Various skills

### 20. Iceberry
- **URL**: https://uppdrag.iceberry.se/
- **Notes**: IT consulting

### 21. ICW
- **URL**: https://www.icw.se/jobs
- **Notes**: IT consulting

### 22. Interim Search
- **URL**: https://www.interimsearch.com/publika-uppdrag/
- **Notes**: Standard job board

### 23. ITC Network
- **URL**: https://itcnetwork.se/uppdrag/
- **Notes**: IT consulting

### 24. Jappa Jobs
- **URL**: https://www.jappa.jobs/jobb
- **Notes**: Modern job board

### 25. Kantur
- **URL**: https://kantur.se/leads
- **Notes**: Lead generation focused

### 26. Keyman
- **URL**: https://keyman.se/uppdrag/
- **Notes**: Key personnel

### 27. Koalitionen
- **URL**: https://koalitionen.com/career/
- **Notes**: Creative industry

### 28. Konsultfabriken
- **URL**: https://www.konsultfabriken.se/all-assignments.php
- **Notes**: PHP-based site

### 29. Pro4u
- **URL**: https://www.pro4u.se/category/uppdragsport
- **Notes**: Blog-style listings

### 30. Partner Network Portal
- **URL**: https://partnernetworkportal.azurewebsites.net/
- **Notes**: Azure-hosted portal

## Implementation Priority Order

### Phase 1 - Quick Wins (Simple HTML)
1. Developers Bay
2. Interim Search  
3. Biolit
4. Konsultfabriken (simple PHP)

### Phase 2 - Standard Job Boards
1. Aliant (Recman)
2. Amendo
3. Epico
4. Great IT
5. Jappa Jobs

### Phase 3 - Specialized Portals
1. Finance Recruitment
2. Digitalenta
3. Gameboost
4. Gigs for Her
5. Koalitionen

### Phase 4 - Complex/Dynamic Sites
1. Centric
2. Evolution Jobs
3. Partner Network Portal
4. DJV Consulting
5. Kantur

## Technical Notes

- Many sites use JavaScript-heavy frameworks
- Some require authentication or have rate limiting
- Consider using StandardJobSiteProvider for simpler sites
- Browser automation may be needed for complex sites
- Check robots.txt and terms of service for each site