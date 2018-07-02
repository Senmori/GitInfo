package net.senmori;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gitective.core.CommitFinder;
import org.gitective.core.filter.commit.AllCommitFilter;
import org.gitective.core.filter.commit.AuthorSetFilter;
import org.gitective.core.filter.commit.CommitCountFilter;
import org.gitective.core.filter.commit.CommitListFilter;
import org.gitective.core.filter.commit.CommitterDateFilter;

import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class Main {

    private static void log(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        try
        {
            Set<String> authors = Sets.newHashSet();
            File gitDir = new File("<git directory>");
            CommitFinder finder = new CommitFinder(gitDir);

            CommitCountFilter counter = new CommitCountFilter();
            CommitterDateFilter dateFilter = new CommitterDateFilter(Instant.now().toEpochMilli());
            AuthorSetFilter authorSetFilter = new AuthorSetFilter();
            CommitListFilter listFilter = new CommitListFilter();
            AllCommitFilter filters = new AllCommitFilter(counter, dateFilter, authorSetFilter, listFilter);

            finder.setFilter(filters);
            finder.findFrom("refs/heads/master");

            authorSetFilter.getPersons().forEach((person) -> authors.add(person.getName()));
            Map<String, Integer> atcMap = Maps.newHashMap(); // author to commit

            Instant earliest = Instant.now();
            for(RevCommit commit : listFilter.getCommits()) {
                Instant commitDate = Instant.ofEpochMilli(commit.getCommitTime() * 1000L);
                if(commitDate.isBefore(earliest)) {
                    earliest = commitDate;
                }
                String name = commit.getAuthorIdent().getName();
                if(!atcMap.containsKey(name)) {
                    atcMap.put(name, 1);
                } else {
                    int count = atcMap.getOrDefault(name, 1);
                    atcMap.put(name, ++count);
                }
            }
            List<String> sorted = atcMap.entrySet().stream()
                                     .sorted(reverseOrder(comparing(Map.Entry::getValue)))
                                     .map(Map.Entry::getKey)
                                     .collect(toList());

            log("#--------------------------#");
            log("There have been " + counter.getCount() + " commits for the project.");
            log(authors.size() + " have contributed to the project.");
            log("The earliest commit was committed on " + new Date(earliest.toEpochMilli()));
            log("#--------------------------#");
            log("Contributor  |  Number of Commits");
            for(String name : sorted) {
                log(name + "  |  " + atcMap.get(name));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
