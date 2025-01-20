package com.example.test.service;

import com.example.test.dto.PersonClientDto;
import com.example.test.dto.PersonClientDtoResponse;
import com.example.test.model.Client;
import com.example.test.model.Person;
import com.example.test.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    private final PersonService personService;

    public Mono<Void> create(PersonClientDto personClientDto) {
        Mono<Person> person = Mono.just(new Person(
                null,
                personClientDto.getPersonName(),
                personClientDto.getPersonGender(),
                personClientDto.getPersonAge(),
                personClientDto.getPersonIdentification(),
                personClientDto.getPersonAddress(),
                personClientDto.getPersonPhone()
        ));
        return person.flatMap(personService::savePerson)
                .flatMap(savedPerson -> {
                    Client client = new Client(
                            null,
                            savedPerson.getPersonId(),
                            personClientDto.getClientPassword(),
                            Boolean.TRUE
                    );
                    return saveClient(client);
                }).then();
    }

    public Mono<Void> update(Integer id, PersonClientDto personClientDto) {
        return personService.getPersonById(id)
                .flatMap(person -> {
                    person.setPersonName(personClientDto.getPersonName());
                    person.setPersonGender(personClientDto.getPersonGender());
                    person.setPersonAge(personClientDto.getPersonAge());
                    person.setPersonIdentification(personClientDto.getPersonIdentification());
                    person.setPersonAddress(personClientDto.getPersonAddress());
                    person.setPersonPhone(personClientDto.getPersonPhone());
                    person.setRegistrationDate(person.getRegistrationDate());
                    return personService.savePerson(person);
                }).flatMap(person -> clientRepository.findClientByPersonClientId(id)
                        .flatMap(client -> {
                            client.setClientPassword(personClientDto.getClientPassword());
                            client.setClientStatus(personClientDto.getClientStatus());
                            client.setRegistrationDate(client.getRegistrationDate());
                            return saveClient(client);
                        })).then();
    }

    public Mono<Client> saveClient(Client client) {
        return clientRepository.save(client);
    }

    public Mono<PersonClientDtoResponse> getClientById(Integer id) {
        return clientRepository.findById(id)
                .flatMap(client -> personService.getPersonById(client.getPersonClientId())
                        .map(person -> new PersonClientDtoResponse(
                                client.getClientId(),
                                person.getPersonId(),
                                person.getPersonName(),
                                person.getPersonAddress(),
                                person.getPersonPhone(),
                                client.getClientPassword(),
                                client.getClientStatus()
                        )));
    }

    public Mono<Void> deleteClientById(Integer id) {
        return clientRepository.findById(id)
                .flatMap(client -> personService.getPersonById(client.getPersonClientId())
                        .flatMap(person -> clientRepository.deleteById(id)
                                .then(personService.deletePersonById(person.getPersonId()))));
    }

    public Flux<PersonClientDtoResponse> getAllClients() {
        return clientRepository.findAll()
                .flatMap(client -> personService.getPersonById(client.getPersonClientId())
                        .map(person -> new PersonClientDtoResponse(
                                client.getClientId(),
                                person.getPersonId(),
                                person.getPersonName(),
                                person.getPersonAddress(),
                                person.getPersonPhone(),
                                client.getClientPassword(),
                                client.getClientStatus()
                        )));
    }
}
