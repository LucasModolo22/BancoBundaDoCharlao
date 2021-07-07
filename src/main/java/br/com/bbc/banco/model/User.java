package br.com.bbc.banco.model;

import br.com.bbc.banco.exception.SaldoInsuficienteException;
import br.com.bbc.banco.exception.ValorInvalidoException;
import br.com.bbc.banco.util.GenericUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private BigDecimal saldo = new BigDecimal(1000);
    private Boolean isAdmin = false;
    private LocalDateTime ultimoDaily = LocalDateTime.now();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserBet> user_bet;

    public User() {}

    public User(Long id){
        this.id = id;
    }

    public Boolean saldoSuficiente(BigDecimal valor) throws Exception {
        if ((this.saldo.subtract(valor)).compareTo(BigDecimal.ZERO) < 0) throw new SaldoInsuficienteException();
        if (valor.compareTo(BigDecimal.ZERO) <= 0) throw new ValorInvalidoException();

        return true;
    }

    public void sacar(BigDecimal valor) throws Exception {
        if (this.saldoSuficiente(valor)) this.saldo = saldo.subtract(valor);
    }

    public void depositar(BigDecimal valor) throws Exception{
        if (valor.compareTo(BigDecimal.ZERO) <= 0) throw new ValorInvalidoException();

        this.saldo = saldo.add(valor);
    }

    public void transferir(BigDecimal valor, User para) throws Exception {
        this.sacar(valor);
        para.depositar(valor);
    }
}
