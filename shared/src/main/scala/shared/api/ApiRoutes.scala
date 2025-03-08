package shared.api

// API routes shared between frontend and backend
object ApiRoutes:
  val BaseUrl = "/api"
  
  object Users:
    val BasePath = s"$BaseUrl/users"
    val Register = s"$BasePath/register" // POST
    val Login = s"$BasePath/login" // POST
    val Profile = s"$BasePath/profile" // GET
  
  object Reservations:
    val BasePath = s"$BaseUrl/reservations"
    def reservation(id: String) = s"$BasePath/$id" // GET, DELETE
    val Create = BasePath // POST
    def byPeriod(startDate: String, endDate: String) = 
      s"$BasePath/period?startDate=$startDate&endDate=$endDate" // GET