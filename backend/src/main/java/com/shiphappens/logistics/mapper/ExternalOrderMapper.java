package com.shiphappens.logistics.mapper;
import com.shiphappens.logistics.dto.Requests; import com.shiphappens.logistics.entity.Customer; import org.springframework.stereotype.Component;
@Component public class ExternalOrderMapper { public Requests.CreateOrder toCreateOrder(Customer customer,Requests.ExternalOrder external){return new Requests.CreateOrder(customer.getId(),external.pickup(),external.delivery(),external.packages());} }
