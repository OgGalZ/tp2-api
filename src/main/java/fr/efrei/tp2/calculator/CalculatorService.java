package fr.efrei.tp2.calculator;

import fr.efrei.tp2.calculator.CalculatorDtos.CalculatorRequest;
import fr.efrei.tp2.calculator.CalculatorDtos.OperationResponse;
import fr.efrei.tp2.common.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalculatorService {

    private final OperationRepository repository;

    public CalculatorService(OperationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OperationResponse calculate(CalculatorRequest request) {
        OperatorType op = OperatorType.from(request.operation());
        double result = compute(op, request.a(), request.b());
        Operation saved = repository.save(new Operation(op, request.a(), request.b(), result));
        return OperationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<OperationResponse> history(Pageable pageable) {
        return repository.findAll(pageable).map(OperationResponse::from);
    }

    @Transactional(readOnly = true)
    public OperationResponse findById(Long id) {
        return OperationResponse.from(getOrThrow(id));
    }

    @Transactional
    public OperationResponse update(Long id, CalculatorRequest request) {
        Operation existing = getOrThrow(id);
        OperatorType op = OperatorType.from(request.operation());
        double result = compute(op, request.a(), request.b());
        existing.update(op, request.a(), request.b(), result);
        return OperationResponse.from(existing);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(getOrThrow(id));
    }

    /** Logique de calcul pure : la division par zéro est refusée explicitement (400). */
    double compute(OperatorType op, double a, double b) {
        return switch (op) {
            case ADDITION -> a + b;
            case SOUSTRACTION -> a - b;
            case MULTIPLICATION -> a * b;
            case DIVISION -> {
                if (b == 0) {
                    throw new ApiException.BadRequest("La division par zéro n'est pas autorisée");
                }
                yield a / b;
            }
        };
    }

    private Operation getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException.NotFound("Opération introuvable avec l'id " + id));
    }
}
