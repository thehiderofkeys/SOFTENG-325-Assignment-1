package se325.assignment01.concert.service.services;

import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.*;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerfomerMapper;
import se325.assignment01.concert.service.mapper.SeatMapper;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Path("/concert-service")
public class ConcertResource {
    // TODO Implement this.
    private static long idCounter = 0;
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    private final static Set<Subscriber> subscriberSet = new HashSet<>();

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

    @GET
    @Path("concerts/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveConcert(@PathParam("id") long id){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        Concert concert = em.find(Concert.class, id);
        em.close();
        if(concert == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(ConcertMapper.toConcertDTO(concert)).build();
    }

    @GET
    @Path("concerts/")
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveConcerts(){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        List<Concert> concerts = em.createQuery("select c from Concert c",Concert.class).getResultList();
        em.close();
        List<ConcertDTO> summaries = new LinkedList<>();
        for(Concert concert : concerts){
            summaries.add(ConcertMapper.toConcertDTO(concert));
        }
        return Response.ok(summaries).build();
    }

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

    @GET
    @Path("seats/{date}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response retrieveSeats(@PathParam("date") String dateString, @QueryParam("status") BookingStatus status){
        LocalDateTime date = new LocalDateTimeParam(dateString).getLocalDateTime();
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        String hql = "select s from Seat s where s.date = :date";
        if(status != BookingStatus.Any)
            hql += " and s.isBooked = :booked";
        TypedQuery<Seat> query = em.createQuery(hql, Seat.class);
        query.setParameter("date",date);
        if(status != BookingStatus.Any)
            query.setParameter("booked", status == BookingStatus.Booked);
        List<Seat> seats = query.getResultList();
        em.close();
        List<SeatDTO> seatDTOs = new LinkedList<>();
        for(Seat seat : seats){
            seatDTOs.add(SeatMapper.toDTO(seat));
        }
        return Response.ok(seatDTOs).build();
    }


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
            updateSubscribers(details.getDate(),concert.getId());
            UriBuilder uriBuilder = uriInfo.getAbsolutePathBuilder();
            uriBuilder.path(Long.toString(booking.getId()));
            response = Response.created(uriBuilder.build()).build();
        }
        catch (NoResultException e) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
        catch (WebApplicationException e){
            em.getTransaction().rollback();
            response = e.getResponse();
        }
        System.out.println("Responce is: " + response.getStatus());
        em.close();
        return response;
    }

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
            performerDTOs.add(PerfomerMapper.toDTO(performer));
        }
        return Response.ok(performerDTOs).build();
    }

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
            response = Response.ok(PerfomerMapper.toDTO(performer)).build();
        }
        catch (NoResultException e){
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        em.close();
        return response;
    }

    @POST
    @Path("subscribe/concertInfo")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public void subscribeConcertInfo(@Suspended AsyncResponse response,
                                     ConcertInfoSubscriptionDTO subscription, @CookieParam("auth") Cookie cookie){
        if(cookie == null)
            throw new WebApplicationException(Response.Status.UNAUTHORIZED);
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        Concert concert = em.find(Concert.class, subscription.getConcertId());
        em.close();
        if(concert == null)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        if(!concert.getDates().contains(subscription.getDate()))
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        long id = subscription.getConcertId();
        int percentage = subscription.getPercentageBooked();
        synchronized (subscriberSet) {
            subscriberSet.add(new Subscriber(id, percentage, response));
        }
    }

    private void updateSubscribers(LocalDateTime date, long id){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        em.getTransaction().begin();
        String hql = "select s from Seat s where s.date = :date";
        TypedQuery<Seat> query = em.createQuery(hql,Seat.class).setParameter("date",date);
        List<Seat> seats = query.getResultList();
        em.close();
        int available = 0;
        for(Seat seat : seats){
            if(!seat.isBooked())
                available++;
        }
        synchronized (subscriberSet) {
            for (Subscriber subscriber : subscriberSet){
                if(subscriber.id == id)
                    subscriber.update(seats.size(),available);
            }
        }
    }

    private class Subscriber{
        long id;
        int percentage;
        AsyncResponse response;
        Subscriber(long id, int percentage, AsyncResponse response){
            this.id = id;
            this.percentage = percentage;
            this.response = response;
        }
        void update(int numSeats, int numAvailable){
            if((numAvailable*100)/numSeats < percentage) {
                threadPool.submit(() ->{
                    response.resume(new ConcertInfoNotificationDTO(numAvailable));
                });
            }
        }
    }
}
