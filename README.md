# care-pathways

## Prerequisites

PhenoTips 1.4+

## Build and installation instructions
1. Build and install PhenoTips 1.4+
1. Build the care-pathwaysl project (mvn clean install)
1. Copy the resulting jars to WEB-INF/lib . Especially when upgrading, ensure only one version of each jar exists in WEB-INF/lib.
1. Restart PhenoTips
1. Import the xar resulting from the build (Administration > Import)

## Care Pathways questionnaires
To do

## Other changes
### Cohorts

As per user's request, 3 patient form templates were configured and included with the Care Pathways code:
* Studies.Phase 1 -- "Care Pathways cohort"; has a short PhenoTips patient form and the Care Pathways questionnaires
* Studies.Phase 2 -- "Unsolved cohort"; this is the default for new patients; has a long PhenoTips patient form and NO Care Pathways questionnaire
* Studies.Phase 2 with CP -- "Unsolved wilth Care Pathways cohort"; this is the default for new patients; has a long PhenoTips patient form and the Care Pathways questionnaires

Additionally, the UI and workflow of the study management has been updated:
- new translation texts (mainly replacing "study" with "cohort"
- removal of the empty default for study reference
- turning Care Pathways questionaires on/off depending on a configuration associated with the study
- new homepage widget listing all cohorts

All of these changes are implemented in the document "CarePathways.StudyCustomizations".
