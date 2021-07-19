package br.com.bbc.banco.command;

import br.com.bbc.banco.configuration.BotApplication;
import br.com.bbc.banco.embed.Embeds;
import br.com.bbc.banco.embed.JokenpoEmbed;
import br.com.bbc.banco.enumeration.BotEnumeration;
import br.com.bbc.banco.enumeration.TransactionType;
import br.com.bbc.banco.exception.PlayerInvalidoException;
import br.com.bbc.banco.model.Jokenpo;
import br.com.bbc.banco.model.Transaction;
import br.com.bbc.banco.model.User;
import br.com.bbc.banco.service.JokenpoService;
import br.com.bbc.banco.service.TransactionService;
import br.com.bbc.banco.service.UserService;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static br.com.bbc.banco.util.GenericUtils.convertStringToBigDecimalReplacingComma;

@Component
public class Jokenpos {

    @Autowired
    private UserService userService;

    @Autowired
    private JokenpoService jokenpoService;

    @Autowired
    private TransactionService transactionService;


    public MessageEmbed jokenpo(net.dv8tion.jda.api.entities.User author, net.dv8tion.jda.api.entities.User other, String valueString) throws Exception {
        if(author.getIdLong() == other.getIdLong()) throw new PlayerInvalidoException();


        User player1 = userService.findOrCreateById(author.getIdLong());
        User player2 = userService.findOrCreateById(other.getIdLong());
        BigDecimal value = convertStringToBigDecimalReplacingComma(valueString);

        Jokenpo jokenpo = new br.com.bbc.banco.model.Jokenpo();
        jokenpo.setPlayer1Id(player1.getId());
        jokenpo.setPlayer2Id(player2.getId());
        jokenpo.setValue(value);

        this.jokenpoService.create(jokenpo);

        Embeds embed = new JokenpoEmbed(other, jokenpo.getId());

        String message = String.format("%s te desafiou!",author.getName());

        String underMessage = String.format("Valor: %s %.2f",
                BotEnumeration.CURRENCY.getText(),
                value
        );

        embed.addField(message,underMessage);

        return embed.build();
    }


    public MessageEmbed aceitaJokenpo(net.dv8tion.jda.api.entities.User author, String jokenpoId) throws PlayerInvalidoException {
        Jokenpo jokenpo = this.jokenpoService.findById(Long.parseLong(jokenpoId));

        if(jokenpo.getPlayer2Id() != author.getIdLong()) throw new PlayerInvalidoException();

        Embeds embed = new JokenpoEmbed(author, Long.parseLong(jokenpoId));
        embed.addField("Escolha uma das opçoes abaixo","");
        return embed.build();
    }

    public void recusaJokenpo(net.dv8tion.jda.api.entities.User author, String jokenpoId, Message message) throws PlayerInvalidoException {
        Jokenpo jokenpo = this.jokenpoService.findById(Long.parseLong(jokenpoId));
        if(jokenpo.getPlayer2Id() != author.getIdLong() && jokenpo.getPlayer1Id() != author.getIdLong()) throw new PlayerInvalidoException();
        message.delete().queue();
    }





    public MessageEmbed escolheOpcao(net.dv8tion.jda.api.entities.User author, String option, long jokenpoId) throws Exception {
        Jokenpo jokenpo = this.jokenpoService.findById(jokenpoId);

        if(jokenpo.getPlayer2Id() != author.getIdLong() && jokenpo.getPlayer1Id() != author.getIdLong()) throw new PlayerInvalidoException();
        else if(jokenpo.getPlayer1Id() == author.getIdLong() && jokenpo.getPlayer1Pick() == null) jokenpo.setPlayer1Pick(option);
        else if(jokenpo.getPlayer2Id() == author.getIdLong() && jokenpo.getPlayer2Pick() == null) jokenpo.setPlayer2Pick(option);

        jokenpoService.update(jokenpo);

        if(jokenpo.getPlayer1Pick() != null && jokenpo.getPlayer2Pick() != null){

            int winnerNumber = jokenpoWinner(jokenpo);

            //Empate
            if(winnerNumber == 0){
                Embeds embed = new JokenpoEmbed(author,jokenpoId);
                embed.addField("Empatou", String.format("Ambos escolheram %s", Emoji.fromUnicode(jokenpo.getPlayer1Pick())));
                return embed.build();
            }

            //Separa Vencedor e Perdedor
            net.dv8tion.jda.api.entities.User winner;
            net.dv8tion.jda.api.entities.User loser;

            if (winnerNumber == 1){
                winner = BotApplication.jda.retrieveUserById(jokenpo.getPlayer1Id()).complete();
                loser = BotApplication.jda.retrieveUserById(jokenpo.getPlayer2Id()).complete();
            }
            else{
                winner = BotApplication.jda.retrieveUserById(jokenpo.getPlayer2Id()).complete();
                loser = BotApplication.jda.retrieveUserById(jokenpo.getPlayer1Id()).complete();
            }

            User userWinner = userService.findOrCreateById(winner.getIdLong());
            User userLoser = userService.findOrCreateById(loser.getIdLong());

            //Faz a transferencia de dinheiro do Perdedor pro Ganhador
            userLoser.transferir(jokenpo.getValue(),userWinner);
            userService.update(userLoser);
            userService.update(userWinner);

            transactionService.create(new Transaction(jokenpo.getValue(),userLoser,userWinner, TransactionType.JOKENPO));


            //Cria o embed de retorno
            Embeds embed = new JokenpoEmbed(winner,jokenpoId);

            String message = String.format("%s Ganhou",
                    winner.getName()
            );
            String messageAfter = String.format("mais sorte da proxima vez %s",
                    loser.getName()
            );
            embed.addField(message,messageAfter);

            return embed.build();

        }

        return null;
    }

    private int jokenpoWinner(Jokenpo jokenpo){
        String pick1 = jokenpo.getPlayer1Pick();
        String pick2 = jokenpo.getPlayer2Pick();

        if(pick1.equals(pick2)) return 0;
        switch (pick1){
            case "pedra":
                if(pick2.equals("papel")) return 2;
                return 1;
            case "papel":
                if(pick2.equals("tesoura")) return 2;
                return 1;
            case "tesoura":
                if(pick2.equals("pedra")) return 2;
                return 1;
            default:
                throw new IllegalStateException("Unexpected value: " + pick1);
        }
    }
}
