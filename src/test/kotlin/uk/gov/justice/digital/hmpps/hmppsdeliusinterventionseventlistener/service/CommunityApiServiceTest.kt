package uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.service

import com.nhaarman.mockitokotlin2.mock
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.component.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Attendance
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.Behaviour
import uk.gov.justice.digital.hmpps.hmppsdeliusinterventionseventlistener.model.crs.SessionFeedback

internal class CommunityApiServiceTest {

  private val communityApiClient = mock<CommunityApiClient>()

  private val communityApiService = CommunityApiService(
    "services.community-api.baseurl",
    "interventions-ui.baseurl",
    communityApiClient,
  )

  @Test
  fun `sets notify PP when non attendance or behaviour notified`() {
    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("no"), Behaviour(null))))
    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("yes"), Behaviour(true))))
    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("late"), Behaviour(true))))

    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("No"), Behaviour(null))))
    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("YES"), Behaviour(true))))
    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("lAtE"), Behaviour(true))))
  }

  @Test
  fun `sets notify PP to false when no attendance nor behaviour issues`() {
    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("yes"), Behaviour(false))))
    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("late"), Behaviour(false))))
    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("yes"), Behaviour(null))))
    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("late"), Behaviour(null))))

    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("YES"), Behaviour(false))))
    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("Late"), Behaviour(false))))
    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("yeS"), Behaviour(null))))
    assertFalse(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("aAtE"), Behaviour(null))))
  }

  @Test
  fun `behaviour not relevant (and hence ignored) if non attendance`() {
    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("no"), Behaviour(true))))
    assertTrue(communityApiService.setNotifyPPIfRequired(SessionFeedback(Attendance("no"), Behaviour(false))))
  }
}
