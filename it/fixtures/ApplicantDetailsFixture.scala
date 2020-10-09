package fixtures

import java.time.LocalDate

import models.TransactorDetails
import models.api.ScrsAddress
import models.external.{Applicant, Name}
import models.view._

trait ApplicantDetailsFixture {


  def generateApplicant(first: String, middle: Option[String], last: String, role: String) = Applicant(
    name = Name(first = Some(first), middle = middle, last = last),
    role = role
  )

  val applicantDob = LocalDate.of(1998, 7, 12)

  val testRole = "03"
  val applicantNino = "ZZ987654A"
  val validApplicant: Applicant = generateApplicant("First", Some("Middle"), "Last", "Director")
  val applicantDetailsPreIv = ApplicantDetails(None, None, None, None, None, None)
  val validCurrentAddress = ScrsAddress(line1 = "TestLine1", line2 = "TestLine2", postcode = Some("TE 1ST"))
  val validPrevAddress = ScrsAddress(line1 = "TestLine11", line2 = "TestLine22", postcode = Some("TE1 1ST"))
  val applicantEmail = "test@test"
  val testFirstName = "testFirstName"
  val testLastName = "testLastName"
  val testApplicantNino = "AB123456C"
  val testApplicantDob = LocalDate.of(2020,1,1)

  val testTransactorDetails = TransactorDetails(testFirstName, testLastName, testApplicantNino, testApplicantDob, Some(testRole))

  val validFullApplicantDetails = ApplicantDetails(
    transactorDetails = Some(testTransactorDetails),
    homeAddress = Some(HomeAddressView(validCurrentAddress.id, Some(validCurrentAddress))),
    contactDetails = Some(ContactDetailsView(Some(applicantEmail), Some("1234"), Some("5678"))),
    formerName = Some(FormerNameView(true, Some("New Name Cosmo"))),
    formerNameDate = Some(FormerNameDateView(LocalDate.of(2000, 7, 12))),
    previousAddress = Some(PreviousAddressView(true, Some(validPrevAddress)))
  )
}
