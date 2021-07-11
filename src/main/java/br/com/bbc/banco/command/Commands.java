package br.com.bbc.banco.command;

import br.com.bbc.banco.embed.Embeds;
import br.com.bbc.banco.enumeration.TransactionType;
import br.com.bbc.banco.model.*;
import br.com.bbc.banco.service.*;
import br.com.bbc.banco.util.GenericUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.List;

@Component
public class Commands {

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BetService betService;

    @Autowired
    private OptionService optionService;



    public void transferir(net.dv8tion.jda.api.entities.User author, String valorString, net.dv8tion.jda.api.entities.User transferido) throws Exception {
        if(author.getIdLong() == transferido.getIdLong()) throw new Exception();

        BigDecimal valor = GenericUtils.convertStringToBigDecimalReplacingComma(valorString);

        User user = userService.findOrCreateById(author.getIdLong());
        User para = userService.findOrCreateById(transferido.getIdLong());

        user.transferir(valor, para);
        this.userService.update(user);
        this.userService.update(para);
        this.transactionService.update(new Transaction(valor,user,para, TransactionType.TRANFERENCIA));
    }

    public MessageEmbed daily(net.dv8tion.jda.api.entities.User author) throws Exception {
        User user = userService.findOrCreateById(author.getIdLong());
        if (user.getUltimoDaily().until(LocalDateTime.now(), ChronoUnit.DAYS) >= 1) {
            Random rand = new Random();
            int valor = Math.round(100 * (rand.nextFloat() + 1));

            user.setSaldo(user.getSaldo().add(new BigDecimal(valor)));
            user.setUltimoDaily(LocalDateTime.now());
            this.userService.update(user);

            return Embeds.dailyEmbed(author, user, valor, 0x00000).build();
        }
        long dif = (user.getUltimoDaily().plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()) / 1000;

        long segundos = dif % 60;
        long minutos = (dif / 60) % 60;
        long horas = (dif / 3600);

        return Embeds.dailyEmbedError(author, horas, minutos, segundos, 0x00000).build();
    }

    public MessageEmbed mostrarExtrato(net.dv8tion.jda.api.entities.User author){
        User user = userService.findOrCreateById(author.getIdLong());

        List<Transaction> transactions = this.transactionService.findByUserId(user.getId());

        return Embeds.extratoEmbed(author, user, transactions, 0x00000).build();
    }

}
