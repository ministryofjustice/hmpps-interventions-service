package uk.gov.justice.digital.hmpps.hmppsinterventionsservice.jpa.entity

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class SampleData {
  companion object {
    // there are tonnes of related tables that need to exist to successfully persist an intervention,
    // this is a helper method that persists them all
    fun persistIntervention(em: TestEntityManager, intervention: Intervention): Intervention {
      em.persist(intervention.dynamicFrameworkContract.serviceCategory)
      em.persist(intervention.dynamicFrameworkContract.serviceProvider)
//      em.persist(intervention.dynamicFrameworkContract.contractEligibility)
      intervention.dynamicFrameworkContract.npsRegion?.let { npsRegion ->
        em.persist(npsRegion)
      }
      intervention.dynamicFrameworkContract.pccRegion?.let { pccRegion ->
        em.persist(pccRegion)
      }
      intervention.dynamicFrameworkContract.pccRegion?.npsRegion?.let { npsRegion ->
        em.persist(npsRegion)
      }

      em.persist(intervention.dynamicFrameworkContract)
      return em.persistAndFlush(intervention)
    }

    fun persistReferral(em: TestEntityManager, referral: Referral): Referral {
      persistIntervention(em, referral.intervention)
      referral.createdBy?.let {
        if (em.find(AuthUser::class.java, it.id) == null) {
          em.persist(it)
        }
      }
      referral.sentBy?.let {
        if (em.find(AuthUser::class.java, it.id) == null) {
          em.persist(it)
        }
      }
      return em.persistAndFlush(referral)
    }

    fun sampleReferral(
      crn: String,
      serviceProviderName: String,
      id: UUID = UUID.randomUUID(),
      referenceNumber: String? = null,
      completionDeadline: LocalDate? = null,
      createdAt: OffsetDateTime = OffsetDateTime.now(),
      createdBy: AuthUser = AuthUser("123456", "delius", "bernard.beaks"),
      sentAt: OffsetDateTime? = null,
      sentBy: AuthUser? = null,
    ): Referral {
      return Referral(
        serviceUserCRN = crn,
        id = id,
        createdAt = createdAt,
        createdBy = createdBy,
        completionDeadline = completionDeadline,
        referenceNumber = referenceNumber,
        sentAt = sentAt,
        sentBy = sentBy,
        intervention = sampleIntervention(
          dynamicFrameworkContract = sampleContract(
            serviceCategory = sampleServiceCategory(desiredOutcomes = emptyList()),
            serviceProvider = sampleServiceProvider(id = serviceProviderName, name = serviceProviderName),
          )
        )
      )
    }

    fun sampleIntervention(
      title: String = "Accommodation Service",
      description: String = "Help find sheltered housing",
      dynamicFrameworkContract: DynamicFrameworkContract,
      id: UUID? = null,
      createdAt: OffsetDateTime? = null,
    ): Intervention {
      return Intervention(
        id = id ?: UUID.randomUUID(),
        createdAt = createdAt ?: OffsetDateTime.now(),
        title = title,
        description = description,
        dynamicFrameworkContract = dynamicFrameworkContract,
      )
    }

    fun sampleContract(
      startDate: LocalDate = LocalDate.of(2020, 12, 1),
      endDate: LocalDate = LocalDate.of(2021, 12, 1),
      serviceCategory: ServiceCategory,
      serviceProvider: ServiceProvider,
      npsRegion: NPSRegion? = null,
      pccRegion: PCCRegion? = null,
    ): DynamicFrameworkContract {
      return DynamicFrameworkContract(
        serviceCategory = serviceCategory,
        serviceProvider = serviceProvider,
        startDate = startDate,
        endDate = endDate,
        minimumAge = 18,
        maximumAge = null,
        allowsMale = true,
        allowsFemale = true,
        npsRegion = npsRegion,
        pccRegion = pccRegion
      )
    }

    fun sampleNPSRegion(
      id: Char = 'G',
      name: String = "South West"
    ): NPSRegion {
      return NPSRegion(
        id = id,
        name = name
      )
    }

    fun samplePCCRegion(
      id: String = "avon-and-somerset",
      name: String = "Avon & Somerset",
      npsRegion: NPSRegion = sampleNPSRegion(),
    ): PCCRegion {
      return PCCRegion(
        id = id,
        name = name,
        npsRegion = npsRegion
      )
    }

    fun sampleServiceProvider(
      id: AuthGroupID = "HARMONY_LIVING",
      name: String = "Harmony Living",
      emailAddress: String = "contact@harmonyLiving.com",
    ): ServiceProvider {
      return ServiceProvider(id, name, emailAddress)
    }

    fun sampleServiceCategory(
      desiredOutcomes: List<DesiredOutcome> = emptyList(),
      name: String = "Accommodation",
      id: UUID = UUID.randomUUID(),
      created: OffsetDateTime = OffsetDateTime.now(),
      complexityLevels: List<ComplexityLevel> = emptyList(),
    ): ServiceCategory {

      return ServiceCategory(
        name = name,
        id = id,
        created = created,
        complexityLevels = complexityLevels,
        desiredOutcomes = desiredOutcomes
      )
    }

    fun sampleDesiredOutcome(
      id: UUID = UUID.randomUUID(),
      description: String = "Outcome 1",
      serviceCategoryId: UUID = UUID.randomUUID()
    ): DesiredOutcome {
      return DesiredOutcome(id, description, serviceCategoryId)
    }

    fun persistPCCRegion(em: TestEntityManager, pccRegion: PCCRegion): PCCRegion {
      em.persist(pccRegion.npsRegion)
      return em.persistAndFlush(pccRegion)
    }
  }
}
