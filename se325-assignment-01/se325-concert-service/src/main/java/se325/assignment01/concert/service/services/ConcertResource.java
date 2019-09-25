package se325.assignment01.concert.service.services;

import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.*;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;
import se325.assignment01.concert.service.mapper.SeatMapper;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/concert-service")
public class ConcertResource {
    // TODO Implement this.
    private static long idCounter = 0;
    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    /***
     * Retrieves the summaries of all the concerts.
     * @return Response 200 OK message containing a list of ConcertSummeryDTO
     */
    @GET
    @Path("concerts/summaries")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveSummaries(){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        List<Concert> concerts = em.createQuery("select c from Concert c",Concert.class).getResultList();
        em.close();
        List<ConcertSummaryDTO> summaries = new LinkedList<>();
        for(Concert concert : concerts){
            summaries.add(ConcertMapper.toConcertSummeryDTO(concert));
        }
        return Response.ok(summaries).build();
    }

    /***
     * Retrieves a Concert matching the id.
     * @param id takes id in the path of the GET request
     * @return Response:    200 - OK if id exist in the database. Containing the ConcertDTO object.
     *                      404 - NOT FOUND if id is not found in the database
     */
    @GET
    @Path("concerts/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveConcert(@PathParam("id") long id){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        try {
            Concert concert = em.find(Concert.class, id); //eager
            if (concert == null)
                return Response.status(Response.Status.NOT_FOUND).build();
            return Response.ok(ConcertMapper.toConcertDTO(concert)).build();
        }
        finally {
            em.close();
        }
    }

    /***
     * Retrieves a list of Concert objects
     * @return Response: 200 - OK Containing a list of ConcertDTO object.
     */
    @GET
    @Path("concerts/")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveConcerts(){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        try {
            List<Concert> concerts = em.createQuery("select c from Concert c", Concert.class).getResultList();
            List<ConcertDTO> dtos = new LinkedList<>();
            for (Concert concert : concerts) {
                dtos.add(ConcertMapper.toConcertDTO(concert));
            }
            return Response.ok(dtos).build();
        }
        finally {
            em.close();
        }
    }

    /***
     * Authenticates the client with a token
     * @param login A UserDTO object, containing username and password.
     * @return Response:    200 - OK if the credentials match. "auth" Cookie created containing the token
     *                      401 - UNAUTHORISED if no matching User with matching credital is found in the database.
     */
    @POST
    @Path("login")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response login(UserDTO login){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        TypedQuery<User> query = em.createQuery("select u from User u where u.username = :username", User.class);
        try {
            User user = query.setParameter("username", login.getUsername()).getSingleResult();
            user.setHash(user.hashCode());
            em.merge(user);
            em.getTransaction().commit();
            em.close();
            if(user != null && login.getPassword().equals(user.getPassword())){
                NewCookie cookie = new NewCookie("auth", String.valueOf(user.hashCode()));
                return Response.ok().cookie(cookie).build();
            }
        }
        catch (NoResultException e){
            em.close();
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    /***
     * Retrieves a list of all the Seats for a concert on a particular day.
     * @param dateString A String representation of LocalDateTime in from the path of the request
     * @param status A BookingStatus enum for optionally filtering the request from the query
     * @return Response:    200 - OK if the dateString is valid. Contains requested SeatDTO object
     *                      404 - NOT FOUND if the dateString does not exist in the database
     *                      400 - BAD REQUEST if the dateString can not be parsed to a LocalDateTime
     *
     */
    @GET
    @Path("seats/{date}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveSeats(@PathParam("date") String dateString, @QueryParam("status") BookingStatus status){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        try {
            LocalDateTime date = new LocalDateTimeParam(dateString).getLocalDateTime();
            String concertHQL = "select c from Concert c where :date member c.dates";
            TypedQuery<Concert> concertQuery = em.createQuery(concertHQL,Concert.class).setParameter("date",date);
            concertQuery.getSingleResult();
            String hql = "select s from Seat s where s.date = :date";
            if (status != BookingStatus.Any)
                hql += " and s.isBooked = :booked";
            TypedQuery<Seat> query = em.createQuery(hql, Seat.class);
            query.setParameter("date", date);
            if (status != BookingStatus.Any)
                query.setParameter("booked", status == BookingStatus.Booked);
            List<Seat> seats = query.getResultList();
            List<SeatDTO> seatDTOs = new LinkedList<>();
            for (Seat seat : seats) {
                seatDTOs.add(SeatMapper.toDTO(seat));
            }
            return Response.ok(seatDTOs).build();
        }
        catch (NoResultException e){
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        catch (DateTimeParseException e){
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        finally {
            em.close();
        }
    }

    /***
     * Creates a new booking.
     * @param details A BookingRequestDTO object containing the booking's details contained in the request.
     * @param uriInfo A UriIfo Object. Automatically bound, containing info about the request's URI.
     * @param cookie A Cookie object. Contains the authentication token.
     * @return Response:    201 - CREATED if Booking was successful. Contains the URI of the Booking.
     *                      400 - BAD REQUEST if the request is not formatted correctly.
     *                      401 - UNAUTHORISED if the client is not logged in.
     *                      403 - FORBIDDEN if on of the seat in the booking is already booked.
     */
    @POST
    @Path("bookings")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response createBooking(BookingRequestDTO details, @Context UriInfo uriInfo, @CookieParam("auth") Cookie cookie){
        Response response = null;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        try {
            if(cookie == null)
                throw new WebApplicationException(Response.Status.UNAUTHORIZED);
            TypedQuery<Concert> cQuery   = em.createQuery("select c from Concert c where c.id = :cid", Concert.class);
            Concert concert = cQuery.setParameter("cid", details.getConcertId()).getSingleResult();
            if(!concert.getDates().contains(details.getDate()))
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            TypedQuery<User> userQuery = em.createQuery("select u from User u where u.hash = :hash",User.class);
            User user = userQuery.setParameter("hash",Integer.parseInt(cookie.getValue())).getSingleResult();
            String hql = "select s from Seat s where s.date = :date and s.label in (:seats)";
            TypedQuery<Seat> query = em.createQuery(hql,Seat.class).setParameter("date",details.getDate());
            List<Seat> available = query.setParameter("seats",details.getSeatLabels()).getResultList();
            if(available.size() != details.getSeatLabels().size())
                throw new WebApplicationException(Response.Status.BAD_REQUEST);
            for(Seat seat: available){
                if(seat.isBooked())
                    throw new WebApplicationException(Response.Status.FORBIDDEN);
                seat.setBooked(true);
                em.merge(seat);
            }
            Booking booking = new Booking(idCounter++, details.getConcertId(),details.getDate(),available,user);
            em.persist(booking);
            em.getTransaction().commit();
            //updateSubscribers(details.getDate(),details.getConcertId());
            UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
            uriBuilder.path(Long.toString(booking.getId()));
            response = Response.created(uriBuilder.build()).build();
        }
        catch (NoResultException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        catch (WebApplicationException e){
            em.getTransaction().rollback();
            response =  e.getResponse();
        }
        System.out.println("Responce is: " + response.getStatus());
        em.close();
        return response;
    }

    /***
     * Retrieves a Booking
     * @param id The id of booking requested, contained in the path.
     * @param cookie A Cookie object. Contains the authentication token.
     * @return Response:    200 - OK if the booking is found and the client is authenticated correctly.
     *                      401 - UNAUTHORISED if the client is not logged in.
     *                      403 - FORBIDDEN if on of the seat in the booking is already booked.
     *                      404 - NOT FOUND if the booking does not exist.
     */
    @GET
    @Path("bookings/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveBooking(@PathParam("id") long id, @CookieParam("auth") Cookie cookie){
        if(cookie == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        int hash = Integer.parseInt(cookie.getValue());
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        Booking booking = em.find(Booking.class, id);
        User user = em.find(User.class, booking.getUser().getId());
        em.close();
        if(user == null||user.hashCode() != hash)
            return Response.status(Response.Status.FORBIDDEN).build();
        return Response.ok(BookingMapper.toDTO(booking)).build();
    }

    /***
     * Retrieved a list of all Bookings made by a user.
     * @param cookie A Cookie object. Contains the authentication token.
     * @return Response:    200 - OK if the client is authenticated correctly. Contains the list of ConcertDTOs
     *                      401 - UNAUTHORISED if the client is not logged in.
     */
    @GET
    @Path("bookings")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveAllBookings(@CookieParam("auth") Cookie cookie){
        if(cookie == null)
            return Response.status(Response.Status.UNAUTHORIZED).build();
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        int hash = Integer.parseInt(cookie.getValue());
        TypedQuery<User> userQuery = em.createQuery("select u from User u where u.hash = :hash",User.class);
        User user = userQuery.setParameter("hash",hash).getSingleResult();
        TypedQuery<Booking> query = em.createQuery("select b from Booking b where b.user = :user",Booking.class);
        List<Booking> bookings = query.setParameter("user",user).getResultList();
        em.close();
        List<BookingDTO> bookingDTOs = new LinkedList<>();
        for (Booking booking : bookings) {
            bookingDTOs.add(BookingMapper.toDTO(booking));
        }
        return Response.ok(bookingDTOs).build();
    }

    /***
     * Retrieves a list of all Performers
     * @return Response:    200 - OK Contains a list of all PerformerDTOs.
     */
    @GET
    @Path("performers")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveAllPerformers(){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        TypedQuery<Performer> query = em.createQuery("select p from Performer p",Performer.class);
        List<Performer> performers = query.getResultList();
        em.close();
        List<PerformerDTO> performerDTOs = new LinkedList<>();
        for (Performer performer: performers) {
            performerDTOs.add(PerformerMapper.toDTO(performer));
        }
        return Response.ok(performerDTOs).build();
    }

    /***
     * Retrieves a Performer
     * @param id The id of the Performer request
     * @return Response: 200 - OK if the Performer is found in the database. Contains the PerformerDTO object.
     *                   404 - NOT FOUND if the Performer is not foudn in the database.
     */
    @GET
    @Path("performers/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrievePerformer(@PathParam("id") long id){
        Response response;
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            TypedQuery<Performer> query = em.createQuery("select p from Performer p where p.id = :id", Performer.class);
            Performer performer = query.setParameter("id", id).getSingleResult();
            response = Response.ok(PerformerMapper.toDTO(performer)).build();
        }
        catch (NoResultException e){
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        em.close();
        return response;
    }

    /***
     * Creates a new Subscription for Concert seat availability
     * @param response AsyncResponse callback for future response.
     * @param subscription ConcertInfoSubscriptionDTO contains the details of the subscription.
     * @param cookie A Cookie object. Contains the authentication token.
     */
    @POST
    @Path("subscribe/concertInfo")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void subscribeConcertInfo(@Suspended AsyncResponse response,
                                     ConcertInfoSubscriptionDTO subscription, @CookieParam("auth") Cookie cookie) {
        if (cookie == null)
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        Concert concert = em.find(Concert.class, subscription.getConcertId());
        em.close();
        if (concert == null)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        if (!concert.getDates().contains(subscription.getDate()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        long id = subscription.getConcertId();
        int percentage = subscription.getPercentageBooked();
        LocalDateTime date = subscription.getDate();
        int numSeats = getNumSeats(date,null);
        threadPool.submit( ()-> {
            while(true){
                int numAvailSeats = getNumSeats(date,"s.isBooked = false");
                if(percentage > (numAvailSeats*100)/numSeats) {
                    response.resume(new ConcertInfoNotificationDTO(numAvailSeats));
                    return;
                }
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e){
                    response.cancel();
                    return;
                }
            }
        });
    }

    /***
     * Helper function to get the current number of seats.
     * @param date Date for the Concert to check the date.
     * @param condition Additional HQL condition for the query. OPTIONAL null for no additional condition.
     * @return The number of seats for the date matching condition.
     */
    private int getNumSeats(LocalDateTime date, String condition){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        String hql = "select s from Seat s where s.date = :date ";
        if(condition != null)
            hql += "and "+condition;
        TypedQuery<Seat> query = em.createQuery(hql,Seat.class);
        List<Seat> seats = query.setParameter("date",date).getResultList();
        em.close();
        return seats.size();
    }

    /***
     * Subscription method using callback instead of polling.
     */
//    @POST
//    @Path("subscribe/concertInfo")
//    @Consumes({MediaType.APPLICATION_JSON})
//    @Produces({MediaType.APPLICATION_JSON})
//    public void subscribeConcertInfo(@Suspended AsyncResponse response,
//                                     ConcertInfoSubscriptionDTO subscription, @CookieParam("auth") Cookie cookie){
//        if(cookie == null)
//            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
//        EntityManager em = PersistenceManager.instance().createEntityManager();
//        em.getTransaction().begin();
//        Concert concert = em.find(Concert.class, subscription.getConcertId());
//        em.close();
//        if(concert == null)
//            throw new WebApplicationException(Response.Status.BAD_REQUEST);
//        if(!concert.getDates().contains(subscription.getDate()))
//            throw new WebApplicationException(Response.Status.BAD_REQUEST);
//        long id = subscription.getConcertId();
//        int percentage = subscription.getPercentageBooked();
//        synchronized (subscriberSet) {
//            subscriberSet.add(new Subscriber(id, percentage, response));
//        }
//    }
//
//    private void updateSubscribers(LocalDateTime date, long id){
//        EntityManager em = PersistenceManager.instance().createEntityManager();
//        em.getTransaction().begin();
//        String hql = "select s from Seat s where s.date = :date";
//        TypedQuery<Seat> query = em.createQuery(hql,Seat.class).setParameter("date",date);
//        List<Seat> seats = query.getResultList();
//        em.close();
//        int available = 0;
//        for(Seat seat : seats){
//            if(!seat.isBooked())
//                available++;
//        }
//        synchronized (subscriberSet) {
//            for (Subscriber subscriber : subscriberSet){
//                if(subscriber.id == id)
//                    subscriber.update(seats.size(),available);
//            }
//        }
//    }
//
//    private class Subscriber{
//        long id;
//        int percentage;
//        AsyncResponse response;
//        Subscriber(long id, int percentage, AsyncResponse response){
//            this.id = id;
//            this.percentage = percentage;
//            this.response = response;
//        }
//        void update(int numSeats, int numAvailable){
//            if((numAvailable*100)/numSeats < percentage) {
//                threadPool.submit(() ->{
//                    response.resume(new ConcertInfoNotificationDTO(numAvailable));
//                });
//            }
//        }
//    }
}
